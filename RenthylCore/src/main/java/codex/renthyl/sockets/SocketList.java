package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.ArrayList;
import java.util.List;

public class SocketList <T extends Socket<R>, R> extends ArrayList<T> implements PointerSocket<List<R>> {

    private final Renderable task;
    private Socket<List<R>> upstream;
    private List<R> resourceList;
    private int activeRefs = 0;

    public SocketList(Renderable task) {
        this.task = task;
    }

    @Override
    public void update(float tpf) {
        forEach(s -> s.update(tpf));
    }

    @Override
    public void setUpstream(Socket<List<R>> upstream) {
        this.upstream = upstream;
    }

    @Override
    public Socket<List<R>> getUpstream() {
        return upstream;
    }

    @Override
    public boolean isAvailableToDownstream() {
        return task.isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return (upstream == null || upstream.isAvailableToDownstream()) && stream().allMatch(Socket::isUpstreamAvailable);
    }

    @Override
    public List<R> acquire() {
        return acquireOrElse(null);
    }

    @Override
    public void reset() {
        if (resourceList != null) {
            resourceList.clear();
        }
        forEach(Socket::reset);
        if (activeRefs != 0) {
            throw new IllegalStateException("Some references were not released.");
        }
    }

    @Override
    public void queue(RenderingQueue queue) {
        if (upstream != null) {
            upstream.queue(queue);
        }
        forEach(s -> s.queue(queue));
        task.queue(queue);
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
        forEach(s -> s.reference(queuePosition));
    }

    @Override
    public void release() {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release();
        }
        forEach(Socket::release);
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
        for (T s : this) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        return usage;
    }

    public List<R> acquireOrElse(R orElse) {
        if (upstream != null && isEmpty()) {
            return upstream.acquire();
        }
        if (resourceList == null) {
            resourceList = new ArrayList<>(size());
        } else {
            resourceList.clear();
        }
        if (upstream != null) {
            resourceList.addAll(upstream.acquire());
        }
        for (T s : this) {
            resourceList.add(orElse == null ? s.acquire() : s.acquire(orElse));
        }
        return resourceList;
    }

}
