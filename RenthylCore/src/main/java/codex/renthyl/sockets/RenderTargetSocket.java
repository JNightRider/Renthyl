package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

public class RenderTargetSocket <T extends Texture> implements Socket<T> {

    private final AllocationSocket<T> texture;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final TextureDef<T> texDef;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private int activeRefs = 0;

    public RenderTargetSocket(Renderable task, ResourceAllocator allocator, TextureDef<T> texDef) {
        this.texture = new AllocationSocket<>(task, allocator, texDef);
        this.frameBuffer = new AllocationSocket<>(task, allocator, bufferDef);
        this.texDef = texDef;
    }

    @Override
    public void update(float tpf) {
        texture.update(tpf);
        frameBuffer.update(tpf);
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return texture.isAvailableToDownstream(queuePosition) && frameBuffer.isAvailableToDownstream(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return texture.isUpstreamAvailable(queuePosition) && frameBuffer.isUpstreamAvailable(queuePosition);
    }

    @Override
    public T acquire() {
        return texture.acquire();
    }

    @Override
    public void resetSocket() {
        if (activeRefs > 0) {
            throw new IllegalStateException("More references than releases.");
        }
        texture.resetSocket();
        frameBuffer.resetSocket();
    }

    @Override
    public int getResourceUsage() {
        return Math.max(texture.getResourceUsage(), frameBuffer.getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        texture.reference(queuePosition);
        frameBuffer.reference(queuePosition);
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        texture.release(queuePosition);
        frameBuffer.release(queuePosition);
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        texture.stage(globals, queue);
        frameBuffer.stage(globals, queue);
    }

    public AllocationSocket<T> getTexture() {
        return texture;
    }

    public AllocationSocket<FrameBuffer> getFrameBuffer() {
        return frameBuffer;
    }

    public TextureDef<T> getTextureDef() {
        return texDef;
    }

    public FrameBufferDef getBufferDef() {
        return bufferDef;
    }

}
