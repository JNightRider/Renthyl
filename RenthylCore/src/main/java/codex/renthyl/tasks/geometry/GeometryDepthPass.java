package codex.renthyl.tasks.geometry;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.AllocationSocket;
import codex.renthyl.sockets.CollectorSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.RenderTask;
import com.jme3.material.RenderState;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class GeometryDepthPass extends RenderTask {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final AllocationSocket<Texture2D> depth;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final RenderState forcedState = new RenderState();

    public GeometryDepthPass(ResourceAllocator allocator) {
        addSocket(geometry);
        depth = addSocket(new AllocationSocket<>(this, allocator, depthDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        forcedState.setColorWrite(false);
    }

    @Override
    protected void renderTask() {

        depthDef.setSize(context.getWidth(), context.getHeight());
        bufferDef.setDepthTarget(depth.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        context.getForcedState().pushValue(forcedState);
        for (GeometryQueue q : geometry.acquire()) {
            q.render(context);
        }

        context.getForcedState().pop();
        context.getFrameBuffer().pop();

    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public Socket<Texture2D> getDepth() {
        return depth;
    }

}
