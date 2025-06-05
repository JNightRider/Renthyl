package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

/**
 * Defines a rendering order where the Barrier's renderable task can only run after
 * the awaited task.
 *
 * @author codex
 */
public class Barrier implements Socket {

    private Renderable await;

    public Barrier() {}
    public Barrier(Renderable await) {
        this.await = await;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return false;
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return await.isRenderingComplete();
    }

    @Override
    public Object acquire() {
        return null;
    }

    @Override
    public void resetSocket() {}

    @Override
    public int getResourceUsage() {
        return 0;
    }

    @Override
    public void reference(int queuePosition) {}

    @Override
    public void release(int queuePosition) {}

    @Override
    public int getActiveReferences() {
        return 0;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {}

    public void setAwait(Renderable await) {
        this.await = await;
    }

    public Renderable getAwait() {
        return await;
    }

}
