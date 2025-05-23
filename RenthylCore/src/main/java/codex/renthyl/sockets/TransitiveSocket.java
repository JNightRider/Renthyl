package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

public class TransitiveSocket<T> implements PointerSocket<T> {

    protected final Renderable task;
    protected Socket<? extends T> upstream;
    protected int activeRefs = 0;
    protected boolean staged = false;

    public TransitiveSocket(Renderable task) {
        this.task = task;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public void setUpstream(Socket<? extends T> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    @Override
    public Socket<? extends T> getUpstream() {
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
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || upstream.isAvailableToDownstream(queuePosition);
    }

    @Override
    public T acquire() {
        return upstream != null ? upstream.acquire() : null;
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
    }

    @Override
    public void resetSocket() {
        if (activeRefs != 0) {
            throw new IllegalStateException("Socket not fully released.");
        }
        staged = false;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (!staged) {
            // set flag first, in anticipation of callback
            staged = true;
            // stage task first, to allow the task to safely modify upstreams
            task.stage(globals, queue);
            if (upstream != null) {
                upstream.stage(globals, queue);
            }
        }
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
