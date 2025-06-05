package codex.renthyl.render.queue;

import codex.renthyl.render.RenderWorker;
import codex.renthyl.render.Renderable;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic rendering queue which can perform basic multithreading given
 * an {@link Executor} to handle the worker threads.
 */
public class BasicRenderingQueue implements RenderingQueue {

    private final Executor service;
    private final List<Worker> activeWorkers = new ArrayList<>();
    private final List<Renderable> staged = new ArrayList<>();
    private final Queue<Renderable> queue = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock();
    private final Condition inactive = lock.newCondition();
    private AppProfiler profiler;
    private Exception error;

    public BasicRenderingQueue() {
        this(null);
    }
    public BasicRenderingQueue(Executor service) {
        this.service = service;
    }

    @Override
    public int stage(Renderable task) {
        staged.add(task);
        return staged.size() - 1;
    }

    @Override
    public void update(float tpf) {
        for (Renderable r : staged) {
            r.update(tpf);
        }
    }

    @Override
    public void prepare() {
        forEach(Renderable::prepare);
    }

    @Override
    public void render(AppProfiler profiler, int workers) {
        this.profiler = profiler;
        if (workers > 1 && service == null) {
            throw new NullPointerException("No executor provided for multithreading.");
        }
        if (workers <= 0) {
            throw new IllegalArgumentException("Cannot have fewer than one worker.");
        }
        while (activeWorkers.size() < workers) {
            activeWorkers.add(new Worker(activeWorkers.size()));
        }
        while (activeWorkers.size() > workers) {
            activeWorkers.removeLast();
        }
        // load from staged to queue
        for (Renderable r : staged) {
            if (r.queueForRender()) {
                queue.add(r);
            }
        }
        // execute renders
        Worker main = activeWorkers.getFirst();
        if (service != null) for (Worker w : activeWorkers) {
            if (w != main) {
                service.execute(w);
            }
        }
        main.run();
        if (error != null) {
            throw new RuntimeException("Rendering failed with an exception: " + error.getMessage(), error);
        }
    }

    private void render(Worker worker) throws InterruptedException, TimeoutException {
        if (activeWorkers.size() == 1) {
            renderSingle(worker);
            return;
        }
        loop: while (!queue.isEmpty() && error == null) {
            int size = queue.size();
            for (Iterator<Renderable> it = queue.iterator(); it.hasNext(); ) {
                if (error != null) {
                    break;
                }
                Renderable ex = it.next();
                // submit task for execution
                if (worker.submit(ex)) {
                    it.remove();
                    worker.render(); // render submitted task
                    lock.lock();
                    inactive.signalAll();
                    lock.unlock();
                    continue loop;
                }
            }
            // worker has become inactive
            // if all workers end up here at once, a deadlock has occured
            lock.lock();
            while (size == queue.size() && error == null) {
                if (!inactive.await(5000, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException("Worker timed out waiting for tasks to complete. Candidates: " + Arrays.toString(queue.toArray()));
                }
            }
            lock.unlock();
        }
    }

    private void renderSingle(Worker worker) throws TimeoutException {
        while (!queue.isEmpty()) {
            Renderable ex = queue.poll();
            if (worker.submit(ex)) {
                if (profiler != null) {
                    profiler.spStep(SpStep.ProcPostQueue, ex.toString());
                }
                worker.render();
            } else {
                throw new TimeoutException("Failed to render next task: " + ex);
            }
        }
    }

    @Override
    public void reset() {
        staged.forEach(Renderable::reset);
        activeWorkers.forEach(Worker::clean);
        queue.clear();
        staged.clear();
        error = null;
    }

    @Override
    public Iterator<Renderable> iterator() {
        return staged.iterator();
    }

    public void submitError(Exception error) {
        if (this.error != null) {
            return;
        }
        lock.lock();
        if (this.error == null) {
            this.error = error;
            inactive.signalAll();
        }
        lock.unlock();
    }

    /**
     * Runnable worker which renders tasks from the queue until none remain.
     */
    public class Worker implements Runnable, RenderWorker {

        private final int index;
        private Renderable task;

        public Worker(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            try {
                BasicRenderingQueue.this.render(this);
            } catch (Exception e) {
                submitError(new RuntimeException("Worker render failed with an exception.", e));
            }
        }

        @Override
        public int getThreadIndex() {
            return index;
        }

        public boolean submit(Renderable task) {
            if (this.task != null || !task.ready(this)) {
                return false;
            }
            this.task = task;
            return true;
        }

        public void render() {
            if (task == null) {
                throw new NullPointerException("No renderable task submitted to execute.");
            }
            try {
                task.render();
            } catch (Exception ex) {
                submitError(ex);
            } finally {
                task = null;
            }
        }

        public void clean() {
            task = null;
        }

    }

}
