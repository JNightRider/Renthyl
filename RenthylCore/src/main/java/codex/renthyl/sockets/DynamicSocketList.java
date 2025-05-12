package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DynamicSocketList <T extends PointerSocket<R>, R> implements PointerSocket<List<R>>, Iterable<T> {

    private final Renderable task;
    private final Supplier<T> factory;
    private final List<T> sockets = new ArrayList<>();
    private Socket<List<R>> upstream;
    private List<R> resourceList;
    private int activeRefs = 0;

    public DynamicSocketList(Renderable task, Supplier<T> factory) {
        this.task = task;
        this.factory = factory;
    }

    @Override
    public void update(float tpf) {
        flushTerminalSockets();
    }

    @Override
    public void setUpstream(Socket<List<R>> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    @Override
    public Socket<List<R>> getUpstream() {
        return upstream;
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return (upstream == null || upstream.isAvailableToDownstream(queuePosition)) && sockets.stream().allMatch(t -> t.isUpstreamAvailable(queuePosition));
    }

    @Override
    public List<R> acquire() {
        return acquireOrElse(null);
    }

    @Override
    public List<R> acquire(List<R> orElse) {
        if (resourceList == null) {
            resourceList = orElse;
        }
        return acquireOrElse(null);
    }

    @Override
    public void reset() {
        if (resourceList != null) {
            resourceList.clear();
        }
        sockets.forEach(Socket::reset);
        if (activeRefs != 0) {
            throw new IllegalStateException("Some references were not released.");
        }
    }

    @Override
    public void queue(RenderingQueue queue) {
        if (upstream != null) {
            upstream.queue(queue);
        }
        sockets.forEach(s -> s.queue(queue));
        task.queue(queue);
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
        sockets.forEach(s -> s.reference(queuePosition));
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
        sockets.forEach(t -> t.release(queuePosition));
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public int getResourceUsage() {
        int usage = activeRefs;
        if (upstream != null) {
            usage = Math.max(usage, upstream.getResourceUsage());
        }
        for (T s : sockets) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        return usage;
    }

    @Override
    public Iterator<T> iterator() {
        return sockets.iterator();
    }

    public Stream<T> stream() {
        return sockets.stream();
    }

    public T get(int index) {
        return sockets.get(index);
    }

    public T getFirst() {
        return sockets.getFirst();
    }

    public T getLast() {
        return sockets.getLast();
    }

    public List<R> acquireOrElse(R orElse) {
        if (upstream != null && sockets.isEmpty()) {
            return upstream.acquire();
        }
        if (resourceList == null) {
            resourceList = new ArrayList<>(sockets.size());
        } else {
            resourceList.clear();
        }
        if (upstream != null) {
            resourceList.addAll(upstream.acquire());
        }
        for (T s : sockets) {
            resourceList.add(orElse == null ? s.acquire() : s.acquire(orElse));
        }
        return resourceList;
    }

    public void add(Socket<R> upstream) {
        T s = factory.get();
        s.setUpstream(upstream);
        sockets.add(s);
    }

    public boolean remove(Socket upstream) {
        return sockets.removeIf(s -> {
            if (s.getUpstream() == upstream) {
                s.setUpstream(null);
                return true;
            }
            return false;
        });
    }

    public void flushTerminalSockets() {
        remove(null);
    }

}
