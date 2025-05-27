package codex.renthylplus.effects;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.tasks.filter.PostProcessFilter;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public abstract class AbstractFilterTask extends RenderTask implements PostProcessFilter {

    private Material material;
    private final OptionalSocket<Texture2D> color = new OptionalSocket<>(this);
    private final OptionalSocket<Texture2D> depth = new OptionalSocket<>(this);
    private final AllocationSocket<Texture2D> result;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final CameraState camera = new CameraState(new Camera(1024, 1024), true);

    public AbstractFilterTask(ResourceAllocator allocator, boolean useDepth) {
        this(allocator, null, useDepth);
    }
    public AbstractFilterTask(ResourceAllocator allocator, Material material, boolean useDepth) {
        this.material = material;
        addSockets(color, depth);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        depth.setEnabled(useDepth);
    }

    @Override
    protected void renderTask() {

        if (material == null) {
            material = createMaterial(context.getAssetManager());
        }

        Texture2D colorTex = color.acquireOrThrow(getClass().getName() + " requires scene color.");
        configureResult(resultDef, colorTex);
        camera.resize(colorTex.getImage().getWidth(), colorTex.getImage().getHeight(), false);
        context.getCamera().pushValue(camera);

        bufferDef.setColorTargets(result.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        int colorSamples = colorTex.getImage().getMultiSamples();
        if (colorSamples > 1) {
            material.setInt("NumSamples", colorSamples);
        } else {
            material.clearParam("NumSamples");
        }
        material.setTexture("Texture", colorTex);
        if (depth.isEnabled()) {
            Texture2D depthTex = depth.acquireOrThrow(getClass().getName() + " requires scene depth.");
            material.setTexture("DepthTexture", depthTex);
            int depthSamples = depthTex.getImage().getMultiSamples();
            if (depthSamples > 1) {
                material.setInt("NumSamplesDepth", depthSamples);
            } else {
                material.clearParam("NumSamplesDepth");
            }
        }
        configureMaterial(material);
        context.renderFullscreen(material);

        context.getCamera().pop();
        context.getFrameBuffer().pop();

    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return color;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return depth;
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return result;
    }

    protected Material createMaterial(AssetManager assetManager) {
        throw new UnsupportedOperationException("Material not defined in constructor.");
    }

    protected void configureResult(TextureDef<Texture2D> def, Texture2D color) {
        Image img = color.getImage();
        def.setSize(img);
        def.setSamples(img.getMultiSamples());
        def.setFormat(img.getFormat());
    }

    protected abstract void configureMaterial(Material material);

    protected TextureDef<Texture2D> getResultDef() {
        return resultDef;
    }

    protected Material getMaterial() {
        return material;
    }

}
