package codex.renthyl.sockets.collections;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;

import java.util.ArrayList;
import java.util.List;

public class SocketList <T extends Socket<R>, R> extends ArrayList<T> implements PointerSocket<List<R>> {

    protected final Renderable task;
    private Socket<? extends List<R>> upstream;
    private List<R> resourceList;
    private int activeRefs = 0;
    private boolean staged = false;

    public SocketList(Renderable task) {
        this.task = task;
    }

    @Override
    public void update(float tpf) {
        forEach(s -> s.update(tpf));
    }

    @Override
    public void setUpstream(Socket<? extends List<R>> upstream) {
        this.upstream = upstream;
    }

    @Override
    public Socket<? extends List<R>> getUpstream() {
        return upstream;
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return (upstream == null || upstream.isAvailableToDownstream(queuePosition)) && stream().allMatch(t -> t.isUpstreamAvailable(queuePosition));
    }

    @Override
    public List<R> acquire() {
        return acquireOrElse(null);
    }

    @Override
    public void resetSocket() {
        if (activeRefs != 0) {
            throw new IllegalStateException("Some references were not released.");
        }
        if (resourceList != null) {
            resourceList.clear();
        }
        forEach(Socket::resetSocket);
        staged = false;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (!staged) {
            staged = true;
            task.preStage(globals);
            if (upstream != null) {
                upstream.stage(globals, queue);
            }
            forEach(s -> s.stage(globals, queue));
            task.stage(globals, queue);
        }
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
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
        forEach(t -> t.release(queuePosition));
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
