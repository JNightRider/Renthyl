package codex.renthyl.tasks.attributes;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.Frame;

public class GlobalAccessor <T> extends Frame implements Socket<T> {

    private final String name;
    private Attribute<? extends T> upstream;
    private int activeRefs = 0;

    public GlobalAccessor(String name) {
        this.name = name;
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return isRenderingComplete() && isUpstreamAvailable(queuePosition);
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
    public void resetSocket() {
        if (activeRefs > 0) {
            throw new IllegalStateException("More references than releases.");
        }
    }

    @Override
    public int getResourceUsage() {
        return upstream != null ? Math.max(activeRefs, upstream.getResourceUsage()) : activeRefs;
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
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    protected void stageSockets(GlobalAttributes globals, RenderingQueue queue) {
        super.stageSockets(globals, queue);
        upstream = globals.get(name);
        if (upstream != null) {
            upstream.stage(globals, queue);
        }
    }

    public String getName() {
        return name;
    }

}
