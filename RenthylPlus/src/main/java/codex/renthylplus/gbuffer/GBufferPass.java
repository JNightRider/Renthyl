package codex.renthylplus.gbuffer;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.RenderTask;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class GBufferPass extends RenderTask {

    public static final String TECHNIQUE = "GBuffer";
    public static final String DEPTH = "DepthGBuffer";

    private final ResourceAllocator allocator;
    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<Camera> camera = new TransitiveSocket<>(this);
    private final SocketList<DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D>, Texture2D> gbuffers = new SocketList<>(this);
    private final AllocationSocket<Texture2D> depth;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final FrameBufferDef bufferDef = new FrameBufferDef();

    public GBufferPass(ResourceAllocator allocator) {
        this.allocator = allocator;
        addSockets(geometry, camera, gbuffers);
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        depth = addSocket(new DefinedAllocationSocket<>(this, allocator, depthDef));
    }

    @Override
    protected void renderTask() {

        Camera cam = camera.acquireOrThrow("Camera required.");
        context.getCamera().pushValue(cam, false);
        int w = context.getWidth();
        int h = context.getHeight();

        Texture2D[] colorTargets = new Texture2D[gbuffers.size()];
        int location = 0;
        for (DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D> g : gbuffers) {
            g.getDef().setSize(w, h);
            colorTargets[location++] = g.acquire();
        }
        bufferDef.setColorTargets(colorTargets);
        depthDef.setSize(w, h);
        bufferDef.setDepthTarget(depth.acquire());

        FrameBuffer fbo = frameBuffer.acquire();
        fbo.setMultiTarget(true);
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers(true, true, true);

        for (GeometryQueue q : geometry.acquire()) {
            q.applySettings(context);
            q.render(context);
            q.restoreSettings(context);
        }

        context.getCamera().pop();
        context.getFrameBuffer().pop();

    }

    public void addBuffer(TextureDef<Texture2D> def) {
        gbuffers.add(new DefinedAllocationSocket<>(this, allocator, def));
    }

    public TextureDef<Texture2D> getDepthDef() {
        return depthDef;
    }

}
