package codex.renthylplus.shadow;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.Socket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

public class ShadowMapSocket implements Socket<ShadowMap> {

    private final AllocationSocket<ShadowMap> shadowMap;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final ShadowMapDef shadowDef = new ShadowMapDef();
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private int activeRefs = 0;

    public ShadowMapSocket(Renderable task, ResourceAllocator allocator) {
        shadowMap = new AllocationSocket<>(task, allocator, shadowDef);
        frameBuffer = new AllocationSocket<>(task, allocator, bufferDef);
    }

    @Override
    public void update(float tpf) {
        shadowMap.update(tpf);
        frameBuffer.update(tpf);
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return shadowMap.isAvailableToDownstream(queuePosition)
                && frameBuffer.isAvailableToDownstream(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return shadowMap.isUpstreamAvailable(queuePosition)
                && frameBuffer.isUpstreamAvailable(queuePosition);
    }

    @Override
    public ShadowMap acquire() {
        return shadowMap.acquire();
    }

    @Override
    public void resetSocket() {
        if (activeRefs > 0) {
            throw new IllegalStateException("More references than releases.");
        }
        shadowMap.resetSocket();
        frameBuffer.resetSocket();
    }

    @Override
    public int getResourceUsage() {
        return Math.max(shadowMap.getResourceUsage(), frameBuffer.getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        shadowMap.reference(queuePosition);
        frameBuffer.reference(queuePosition);
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        shadowMap.release(queuePosition);
        frameBuffer.release(queuePosition);
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        shadowMap.stage(globals, queue);
        frameBuffer.stage(globals, queue);
    }

    public void setSize(int width, int height) {
        shadowDef.getMapDef().setSize(width, height);
    }

    public void setTargetDepth(Texture2D depth) {
        bufferDef.setDepthTarget(depth);
    }

    public AllocationSocket<ShadowMap> getShadowMap() {
        return shadowMap;
    }

    public AllocationSocket<FrameBuffer> getFrameBuffer() {
        return frameBuffer;
    }

    public ShadowMapDef getShadowDef() {
        return shadowDef;
    }

    public FrameBufferDef getBufferDef() {
        return bufferDef;
    }

}
