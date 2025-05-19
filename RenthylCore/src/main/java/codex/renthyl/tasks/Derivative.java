package codex.renthyl.tasks;

import codex.renthyl.sockets.Socket;

import java.util.function.Function;

public abstract class Derivative<In, Out> extends RenderTask implements Socket<Out>, Function<In, Out> {

    private Socket<? extends In> upstream;
    private int activeRefs = 0;

    @Override
    protected void renderTask() {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || upstream.isAvailableToDownstream(queuePosition);
    }

    @Override
    public Out acquire() {
        return upstream != null ? apply(upstream.acquire()) : null;
    }

    @Override
    public int getResourceUsage() {
        return upstream == null ? activeRefs : Math.max(activeRefs, upstream.getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
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
    public int getActiveReferences() {
        return activeRefs;
    }

    public void setUpstream(Socket<? extends In> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    public Socket<? extends In> getUpstream() {
        return upstream;
    }

}
