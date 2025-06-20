package codex.renthyljme.effects.ports;

import codex.renthyl.sockets.*;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.effects.AbstractFilterTask;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyljme.tasks.filter.PostProcessFilter;
import codex.renthyl.tasks.utils.Multiplexor;
import codex.renthyl.tasks.Frame;
import codex.renthyljme.tasks.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.post.filters.BloomFilter;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class BloomPass extends Frame implements PostProcessFilter {

    private final TransitiveSocket<Texture2D> sceneColor = new TransitiveSocket<>(this);
    private final OptionalSocket<Texture2D> sceneDepth = new OptionalSocket<>(this, false);
    private final ArgumentSocket<Float> scale = new ArgumentSocket<>(this, 1.5f);
    private final ArgumentSocket<Float> downSamplingFactor = new ArgumentSocket<>(this, 1f);
    private final PerObjectGlowPass objectGlow;
    private final GlowMapSwitch glowSwitch = new GlowMapSwitch(BloomFilter.GlowMode.Scene);
    private final ExtractionPass extraction;
    private final InjectionPass inject;

    public BloomPass(AssetManager assetManager, ResourceAllocator allocator) {
        objectGlow = new PerObjectGlowPass(allocator);
        extraction = new ExtractionPass(assetManager, allocator);
        GaussianBlurPass hBlur = new GaussianBlurPass(assetManager, allocator, false);
        GaussianBlurPass vBlur = new GaussianBlurPass(assetManager, allocator, true);
        inject = new InjectionPass(assetManager, allocator);
        addSockets(sceneColor, sceneDepth, scale, downSamplingFactor);
        glowSwitch.addUpstream(objectGlow.color);
        extraction.getSceneColor().setUpstream(sceneColor);
        extraction.glow.setUpstream(glowSwitch);
        extraction.mode.setUpstream(glowSwitch.mode);
        hBlur.getSceneColor().setUpstream(extraction.getFilterResult());
        hBlur.scale.setUpstream(scale);
        hBlur.downSamplingFactor.setUpstream(downSamplingFactor);
        vBlur.getSceneColor().setUpstream(hBlur.getFilterResult());
        vBlur.scale.setUpstream(scale);
        vBlur.downSamplingFactor.setUpstream(downSamplingFactor);
        inject.getSceneColor().setUpstream(sceneColor);
        inject.bloom.setUpstream(vBlur.getFilterResult());
    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return sceneColor;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return sceneDepth;
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return inject.getFilterResult();
    }

    public ArgumentMacro<BloomFilter.GlowMode> getGlowMode() {
        return glowSwitch.mode;
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return objectGlow.geometry;
    }

    public ArgumentSocket<Float> getExposurePower() {
        return extraction.exposurePower;
    }

    public ArgumentSocket<Float> getExposureCutoff() {
        return extraction.exposureCutoff;
    }

    public ArgumentSocket<Float> getBlurScale() {
        return scale;
    }

    public ArgumentSocket<Float> getDownSamplingFactor() {
        return downSamplingFactor;
    }

    public ArgumentSocket<Float> getIntensity() {
        return inject.intensity;
    }

    private static class PerObjectGlowPass extends RasterTask {

        private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
        private final AllocationSocket<Texture2D> color, depth;
        private final AllocationSocket<FrameBuffer> frameBuffer;
        private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
        private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
        private final FrameBufferDef bufferDef = new FrameBufferDef();

        public PerObjectGlowPass(ResourceAllocator allocator) {
            super();
            addSocket(geometry);
            color = addSocket(new AllocationSocket<>(this, allocator, colorDef));
            depth = addSocket(new AllocationSocket<>(this, allocator, depthDef));
            frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        }

        @Override
        protected void renderTask() {

            colorDef.setSize(context.getWidth(), context.getHeight());
            depthDef.setSize(colorDef);
            bufferDef.setColorTargets(color.acquire());
            bufferDef.setDepthTarget(depth.acquire());

            FrameBuffer fbo = frameBuffer.acquire();
            context.getFrameBuffer().pushValue(fbo);
            context.getForcedTechnique().pushValue("Glow");

            for (GeometryQueue q : geometry.acquire()) {
                q.render(context);
            }

            context.getForcedTechnique().pop();
            context.getFrameBuffer().pop();

        }

    }

    private static class GlowMapSwitch extends Multiplexor<Texture2D> {

        private final ArgumentMacro<BloomFilter.GlowMode> mode = new ArgumentMacro<>();

        public GlowMapSwitch(BloomFilter.GlowMode mode) {
            addSocket(this.mode).setValue(mode);
        }

        @Override
        protected int getNextIndex(int index) {
            return mode.preview() != BloomFilter.GlowMode.Scene ? 0 : -1;
        }

    }

    private static class ExtractionPass extends AbstractFilterTask {

        private final TransitiveSocket<BloomFilter.GlowMode> mode = new TransitiveSocket<>(this);
        private final TransitiveSocket<Texture2D> glow = new TransitiveSocket<>(this);
        private final ArgumentSocket<Float> exposurePower = new ArgumentSocket<>(this, 5.0f);
        private final ArgumentSocket<Float> exposureCutoff = new ArgumentSocket<>(this, 0.0f);

        public ExtractionPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "Common/MatDefs/Post/BloomExtract.j3md"), false);
        }

        @Override
        protected void configureMaterial(Material material) {
            exposurePower.acquireToMaterial(material, "ExposurePow");
            exposureCutoff.acquireToMaterial(material, "ExposureCutoff");
            glow.acquireToMaterial(material, "GlowMap");
            BloomFilter.GlowMode m = mode.acquireOrThrow();
            material.setBoolean("Extract", m != BloomFilter.GlowMode.Objects);
        }

    }

    private static class GaussianBlurPass extends AbstractFilterTask {

        private final boolean vertical;
        private final ArgumentSocket<Float> scale = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> downSamplingFactor = new ArgumentSocket<>(this);

        public GaussianBlurPass(AssetManager assetManager, ResourceAllocator allocator, boolean vertical) {
            super(allocator, new Material(assetManager, "Common/MatDefs/Blur/" + (vertical ? 'V' : 'H') + "GaussianBlur.j3md"), false);
            this.vertical = vertical;
            addSockets(scale, downSamplingFactor);
        }

        @Override
        protected void configureMaterial(Material material) {
            scale.acquireToMaterial(material, "Scale");
            material.setFloat("Size", Math.max(1f, (getDemension() / downSamplingFactor.acquire())));
        }

        private int getDemension() {
            Image colorImg = getSceneColor().acquireOrThrow("Scene color required.").getImage();
            return vertical ? colorImg.getHeight() : colorImg.getWidth();
        }

    }

    private static class InjectionPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> bloom = new TransitiveSocket<>(this);
        private final ArgumentSocket<Float> intensity = new ArgumentSocket<>(this, 2f);

        public InjectionPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "Common/MatDefs/Post/BloomFinal.j3md"), false);
            addSockets(bloom, intensity);
        }

        @Override
        protected void configureMaterial(Material material) {
            bloom.acquireToMaterial(material, "BloomTex");
            intensity.acquireToMaterial(material, "BloomIntensity");
        }

    }

}
