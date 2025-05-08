package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Consumer;

public class TransitiveSocket<T> implements PointerSocket<T> {

    protected final Renderable task;
    protected Socket<T> upstream;
    protected int activeRefs = 0;

    public TransitiveSocket(Renderable task) {
        this.task = task;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public void setUpstream(Socket<T> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    @Override
    public Socket<T> getUpstream() {
        return upstream;
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
    }

    @Override
    public boolean isAvailableToDownstream() {
        return task.isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return upstream == null || upstream.isAvailableToDownstream();
    }

    @Override
    public T acquire() {
        return upstream != null ? upstream.acquire() : null;
    }

    @Override
    public void release() {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release();
        }
    }

    @Override
    public void reset() {
        if (activeRefs != 0) {
            throw new IllegalStateException("Socket not fully released.");
        }
    }

    @Override
    public void queue(RenderingQueue queue) {
        // queue upstream before queueing owning task
        if (upstream != null) {
            upstream.queue(queue);
        }
        task.queue(queue);
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public int getResourceUsage() {
        return upstream == null ? activeRefs : Math.max(activeRefs, upstream.getResourceUsage());
    }

}
