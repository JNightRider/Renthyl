/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyljme.filter.ports;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.tasks.Frame;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.filter.AbstractFilterTask;
import codex.renthyljme.filter.PostProcessFilter;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class SSAOPass extends Frame implements PostProcessFilter {

    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final AOPass ssao;
    private final BlurPass blur;

    /**
     * Create a Screen Space Ambient Occlusion Filter
     */
    public SSAOPass(AssetManager assetManager, ResourceAllocator allocator) {
        this(assetManager, allocator, 5.1f, 1.5f, 0.2f, 0.1f);
    }

    /**
     * Create a Screen Space Ambient Occlusion Filter
     * @param radius The radius of the area where random samples will be picked. default 5.1f
     * @param intensity intensity of the resulting AO. default 1.2f
     * @param scale distance between occluders and occludee. default 0.2f
     * @param bias the width of the occlusion cone considered by the occludee. default 0.1f
     */
    public SSAOPass(AssetManager assetManager, ResourceAllocator allocator, float radius, float intensity, float scale, float bias) {
        addSockets(color);
        ssao = new AOPass(assetManager, allocator);
        blur = new BlurPass(assetManager, allocator);
        ssao.getSceneColor().setUpstream(color);
        ssao.radius.setValue(radius);
        ssao.intensity.setValue(intensity);
        ssao.scale.setValue(scale);
        ssao.bias.setValue(bias);
        blur.getSceneColor().setUpstream(color);
        blur.ssao.setUpstream(ssao.getFilterResult());
        blur.frustumNearFar.setUpstream(ssao.frustumNearFar);
    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return color;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return ssao.getSceneDepth();
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return blur.getFilterResult();
    }

    public PointerSocket<Texture2D> getNormals() {
        return ssao.normals;
    }

    public ArgumentSocket<Float> getRadius() {
        return ssao.radius;
    }

    public ArgumentSocket<Float> getIntensity() {
        return ssao.intensity;
    }

    public ArgumentSocket<Float> getScale() {
        return ssao.scale;
    }

    public ArgumentSocket<Float> getBias() {
        return ssao.bias;
    }

    public ArgumentSocket<Float> getDownSamplingFactor() {
        return ssao.downSamplingFactor;
    }

    public ArgumentSocket<Vector2f[]> getSamples() {
        return ssao.samples;
    }

    public ArgumentSocket<Boolean> getUseAO() {
        return blur.useAo;
    }

    public ArgumentSocket<Boolean> getUseOnlyAO() {
        return blur.onlyAo;
    }

    private static class AOPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> normals = new TransitiveSocket<>(this);
        private final ArgumentSocket<Float> radius = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> intensity = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> scale = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> bias = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> downSamplingFactor = new ArgumentSocket<>(this, 1f);
        private final ArgumentSocket<Vector2f[]> samples = new ArgumentSocket<>(this,
                new Vector2f[] {new Vector2f(1.0f, 0.0f), new Vector2f(-1.0f, 0.0f), new Vector2f(0.0f, 1.0f), new Vector2f(0.0f, -1.0f)});
        private final ValueSocket<Vector2f> frustumNearFar = new ValueSocket<>(this, new Vector2f());
        private final Vector3f frustumCorner = new Vector3f();

        public AOPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "Common/MatDefs/SSAO/ssao.j3md"), true);
            addSockets(normals, radius, intensity, scale, bias, samples, frustumNearFar);
        }

        @Override
        protected void configureResult(TextureDef<Texture2D> def, Texture2D color) {
            Image img = color.getImage();
            float downsample = downSamplingFactor.acquireOrThrow();
            def.setWidth((int)(img.getWidth() / downsample));
            def.setHeight((int)(img.getHeight() / downsample));
            def.setSamples(img.getMultiSamples());
            def.setFormat(img.getFormat());
            Camera cam = context.getCamera().getValue().getCamera();
            float farY = (cam.getFrustumTop() / cam.getFrustumNear()) * cam.getFrustumFar();
            float farX = farY * ((float)def.getWidth() / def.getHeight());
            frustumCorner.set(farX, farY, cam.getFrustumFar());
            frustumNearFar.getValue().set(cam.getFrustumNear(), cam.getFrustumFar());
        }

        @Override
        protected void configureMaterial(Material material) {
            material.setBoolean("ApproximateNormals", normals.acquireToMaterial(material, "Normals") == null);
            radius.acquireToMaterial(material, "SampleRadius");
            intensity.acquireToMaterial(material, "Intensity");
            scale.acquireToMaterial(material, "Scale");
            bias.acquireToMaterial(material, "Bias");
            samples.acquireToMaterial(material, "Samples");
            material.setVector3("FrustumCorner", frustumCorner);
            material.setVector2("FrustumNearFar", frustumNearFar.getValue());
        }

    }

    private static class BlurPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> ssao = new TransitiveSocket<>(this);
        private final TransitiveSocket<Vector2f> frustumNearFar = new TransitiveSocket<>(this);
        private final ArgumentSocket<Boolean> useAo = new ArgumentSocket<>(this, true);
        private final ArgumentSocket<Boolean> onlyAo = new ArgumentSocket<>(this, false);

        public BlurPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "Common/MatDefs/SSAO/ssaoBlur.j3md"), false);
            addSockets(ssao, frustumNearFar, useAo, onlyAo);
        }

        @Override
        protected void configureMaterial(Material material) {
            ssao.acquireToMaterial(material, "SSAOMap");
            useAo.acquireToMaterial(material, "UseAo");
            onlyAo.acquireToMaterial(material, "UseOnlyAo");
            frustumNearFar.acquireToMaterial(material, "FrustumNearFar");
            material.setFloat("XScale", 2f / getResultDef().getWidth());
            material.setFloat("YScale", 2f / getResultDef().getHeight());
        }

    }
    
}
