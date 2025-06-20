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
package codex.renthyljme.effects.ports;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.effects.AbstractFilterTask;
import codex.renthyljme.tasks.filter.PostProcessFilter;
import codex.renthyl.tasks.Frame;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Screenspace reflection pass.
 * 
 * @author riccardoblb (shaders)
 * @author neph1 (JME filter)
 * @author codex (Renthyl adaptation)
 */
public class SSRPass extends Frame implements PostProcessFilter {

    private final AssetManager assetManager;
    private final ResourceAllocator allocator;
    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> result = new TransitiveSocket<>(this);
    private final ArgumentMacro<Integer> numBlurPasses = new ArgumentMacro<>(5);
    private final ArgumentSocket<Boolean> fastBlur = new ArgumentSocket<>(this, false);
    private final ArgumentSocket<Float> blurScale = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> blurSigma = new ArgumentSocket<>(this, 5f);
    private final ReflectionPass reflection;
    private final List<BlurPass> blurPasses = new ArrayList<>();

    public SSRPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(color, result, numBlurPasses, fastBlur, blurScale, blurSigma);
        this.assetManager = assetManager;
        this.allocator = allocator;
        reflection = new ReflectionPass(assetManager, allocator);
        reflection.getSceneColor().setUpstream(color);
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        if (!prestaged) {
            int passes = numBlurPasses.preview();
            if (passes != blurPasses.size()) {
                if (passes > 0) {
                    if (blurPasses.isEmpty()) {
                        createBlur(0).ssr.setUpstream(reflection.getFilterResult());
                    }
                    while (blurPasses.size() < passes) {
                        BlurPass b = blurPasses.getLast();
                        BlurPass blur = createBlur(blurPasses.size());
                        blur.getSceneColor().setUpstream(b.getFilterResult());
                        blur.ssr.setUpstream(b.getFilterResult());
                    }
                    while (blurPasses.size() > passes) {
                        blurPasses.removeLast();
                    }
                    result.setUpstream(blurPasses.getLast().getFilterResult());
                    blurPasses.getLast().getSceneColor().setUpstream(color);
                } else {
                    result.setUpstream(reflection.getFilterResult());
                    blurPasses.clear();
                }
            }
            if (result.getUpstream() == null) {
                result.setUpstream(reflection.getFilterResult());
            }
        }
        super.preStage(globals);
    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return color;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return reflection.getSceneDepth();
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return result;
    }

    private BlurPass createBlur(int i) {
        BlurPass b = new BlurPass(assetManager, allocator, (i & 1) == 0);
        b.fastBlur.setUpstream(fastBlur);
        b.scale.setUpstream(blurScale);
        b.sigma.setUpstream(blurSigma);
        blurPasses.add(b);
        return b;
    }

    public PointerSocket<Texture2D> getNormals() {
        return reflection.normals;
    }

    public ArgumentSocket<Integer> getRaySteps() {
        return reflection.raySteps;
    }

    public ArgumentSocket<Boolean> getSampleNearby() {
        return reflection.sampleNearby;
    }

    public ArgumentSocket<Float> getStepLength() {
        return reflection.stepLength;
    }

    public ArgumentSocket<Float> getReflectionFactor() {
        return reflection.reflectionFactor;
    }

    public ArgumentSocket<Boolean> getGlossinessPackedInNormals() {
        return reflection.glossinessPackedInNormals;
    }

    public ArgumentSocket<Boolean> getApproximateNormals() {
        return reflection.approximateNormals;
    }

    public ArgumentSocket<Vector2f> getNearFade() {
        return reflection.nearFade;
    }

    public ArgumentSocket<Vector2f> getFarFade() {
        return reflection.farFade;
    }

    public ArgumentSocket<Image.Format> getReflectionFormat() {
        return reflection.format;
    }

    public ArgumentSocket<Float> getDownSamplingFactor() {
        return reflection.downSamplingFactor;
    }

    public ArgumentMacro<Integer> getNumBlurPasses() {
        return numBlurPasses;
    }

    public ArgumentSocket<Boolean> getFastBlur() {
        return fastBlur;
    }

    public ArgumentSocket<Float> getBlurScale() {
        return blurScale;
    }

    public ArgumentSocket<Float> getBlurSigma() {
        return blurSigma;
    }

    private static class ReflectionPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> normals = new TransitiveSocket<>(this);
        private final ArgumentSocket<Integer> raySteps = new ArgumentSocket<>(this, 16);
        private final ArgumentSocket<Boolean> sampleNearby = new ArgumentSocket<>(this, true);
        private final ArgumentSocket<Float> stepLength = new ArgumentSocket<>(this, 1.0f);
        private final ArgumentSocket<Float> reflectionFactor = new ArgumentSocket<>(this, 1.0f);
        private final ArgumentSocket<Boolean> glossinessPackedInNormals = new ArgumentSocket<>(this, true);
        private final ArgumentSocket<Boolean> approximateNormals = new ArgumentSocket<>(this, false);
        private final ArgumentSocket<Vector2f> nearFade = new ArgumentSocket<>(this, new Vector2f(0.01f, 1.0f));
        private final ArgumentSocket<Vector2f> farFade = new ArgumentSocket<>(this, new Vector2f(200f, 300f));
        private final ArgumentSocket<Image.Format> format = new ArgumentSocket<>(this, Image.Format.RGBA16F);
        private final ArgumentSocket<Float> downSamplingFactor = new ArgumentSocket<>(this, 1f);

        public ReflectionPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "RenthylJme/MatDefs/Effects/SSR.j3md"), true);
            addSockets(normals, raySteps, sampleNearby, stepLength, reflectionFactor, glossinessPackedInNormals,
                    approximateNormals, nearFade, farFade, format, downSamplingFactor);
        }

        @Override
        protected void configureResult(TextureDef<Texture2D> def, Texture2D color) {
            float downsample = downSamplingFactor.acquireOrThrow();
            def.setSize((int)(def.getWidth() / downsample), (int)(def.getHeight() / downsample));
            def.setFormat(format.acquireOrThrow());
        }

        @Override
        protected void configureMaterial(Material material) {
            normals.acquireToMaterial(material, "Normals");
            raySteps.acquireToMaterial(material, "RaySamples");
            material.setInt("NearbySamples", sampleNearby.acquireOrThrow() ? 4 : 0);
            stepLength.acquireToMaterial(material, "StepLength");
            reflectionFactor.acquireToMaterial(material, "ReflectionFactor");
            glossinessPackedInNormals.acquireToMaterial(material, "GlossinessPackedInNormalB");
            glossinessPackedInNormals.acquireToMaterial(material, "RGNormalMap");
            approximateNormals.acquireToMaterial(material, "ApproximateNormals");
            nearFade.acquireToMaterial(material, "NearReflectionsFade");
            farFade.acquireToMaterial(material, "FarReflectionsFade");
        }

    }

    private static class BlurPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> ssr = new TransitiveSocket<>(this);
        private final ArgumentSocket<Boolean> fastBlur = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> scale = new ArgumentSocket<>(this);
        private final ArgumentSocket<Float> sigma = new ArgumentSocket<>(this);

        public BlurPass(AssetManager assetManager, ResourceAllocator allocator, boolean horizontal) {
            super(allocator, new Material(assetManager, "RenthylJme/MatDefs/Effects/SSRBlur.j3md"), false);
            addSockets(ssr, fastBlur, scale, sigma);
            getMaterial().setBoolean("Horizontal", horizontal);
        }

        @Override
        protected void configureMaterial(Material material) {
            ssr.acquireToMaterial(material, "SSR");
            fastBlur.acquireToMaterial(material, "FastBlur");
            scale.acquireToMaterial(material, "BlurScale");
            sigma.acquireToMaterial(material, "Sigma");
        }

    }
    
}
