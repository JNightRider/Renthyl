package codex.renthyl.render;

import codex.renthyl.FGRenderContext;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BasicRenderingQueue implements RenderingQueue {

    private final Executor service;
    private final List<Worker> activeWorkers = new ArrayList<>();
    private final List<Renderable> staged = new ArrayList<>();
    private final Queue<Renderable> queue = new ConcurrentLinkedQueue<>();
    private final Lock lock = new ReentrantLock();
    private final Condition inactive = lock.newCondition();
    private Exception error;

    public BasicRenderingQueue() {
        this(null);
    }
    public BasicRenderingQueue(Executor service) {
        this.service = service;
    }

    @Override
    public int add(Renderable r) {
        staged.add(r);
        return queue.size() - 1;
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
    public void render(int workers, FGRenderContext context) {
        if (workers > 1 && service == null) {
            throw new NullPointerException("No executor available for multithreading.");
        }
        if (workers <= 0) {
            throw new IllegalArgumentException("Cannot have fewer than one worker.");
        }
        while (activeWorkers.size() < workers) {
            activeWorkers.add(new Worker(activeWorkers.size(), context));
        }
        while (activeWorkers.size() > workers) {
            activeWorkers.removeLast();
        }
        queue.addAll(staged);
        Worker main = activeWorkers.getFirst();
        if (service != null) for (Worker w : activeWorkers) {
            if (w != main) {
                service.execute(w);
            }
        }
        main.run();
        if (error != null) {
            throw new RuntimeException("Rendering failed with exception: " + error.getMessage(), error);
        }
    }

    private void render(Worker worker) throws InterruptedException, TimeoutException {
        while (!queue.isEmpty() && error == null) {
            int size = queue.size();
            for (Iterator<Renderable> it = queue.iterator(); it.hasNext(); ) {
                if (error != null) {
                    break;
                }
                Renderable ex = it.next();
                // submit task for execution
                if (worker.submit(ex)) {
                    worker.render(); // render submitted task
                    it.remove();
                    lock.lock();
                    inactive.signalAll();
                    lock.unlock();
                    break;
                }
            }
            if (size != queue.size() || error != null) {
                continue;
            }
            // worker has become inactive
            // if all workers end up here at once, a deadlock has occured
            lock.lock();
            while (size == queue.size() && error == null) {
                if (!inactive.await(5000, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException("Worker timed out waiting for tasks to complete.");
                }
            }
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        forEach(Renderable::reset);
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

    public class Worker implements Runnable, RenderWorker {

        private final int index;
        private final FGRenderContext context;
        private Renderable task;
        private boolean complete = false;

        public Worker(int index, FGRenderContext context) {
            this.index = index;
            this.context = context;
        }

        @Override
        public void run() {
            try {
                BasicRenderingQueue.this.render(this);
            } catch (TimeoutException e) {
                submitError(new RuntimeException("Worker timed out waiting for runnable tasks.", e));
            } catch (InterruptedException e) {
                submitError(new RuntimeException("Worker interrupted waiting for runnable tasks.", e));
            }
        }

        @Override
        public int getThreadIndex() {
            return index;
        }

        public boolean submit(Renderable task) {
            if (this.task != null || complete || !task.claim(this)) {
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
                task.render(context);
            } catch (Exception ex) {
                submitError(ex);
            } finally {
                task = null;
            }
        }

        public void clean() {
            task = null;
            complete = false;
        }

    }

}
