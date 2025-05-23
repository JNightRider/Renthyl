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
package codex.renthylplus.effects.ports;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyl.tasks.Frame;
import codex.renthyl.tasks.PostProcessFilter;
import codex.renthyl.tasks.RenderTask;
import codex.renthylplus.effects.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.List;

public class SoftBloomPass extends Frame implements PostProcessFilter {

    private final ResourceAllocator allocator;
    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> depth = new OptionalSocket<>(this, false);
    private final TransitiveSocket<Texture2D> result = new TransitiveSocket<>(this);
    private final ArgumentMacro<Integer> numSamplingSteps = new ArgumentMacro<>(1);
    private final ArgumentSocket<Boolean> bilinear = new ArgumentSocket<>(this, true);
    private final List<BlurPass> downsamples = new ArrayList<>();
    private final List<BlurPass> upsamples = new ArrayList<>();
    private final Material downsampleMat, upsampleMat;
    private final ArrayList<CameraState> cameras = new ArrayList<>();
    private final Vector2f tempTexelSize = new Vector2f();
    private final InjectionPass inject;

    public SoftBloomPass(AssetManager assetManager, ResourceAllocator allocator) {
        this.allocator = allocator;
        addSockets(color, depth, result, numSamplingSteps, bilinear);
        downsampleMat = new Material(assetManager, "Common/MatDefs/Post/Downsample.j3md");
        upsampleMat = new Material(assetManager, "Common/MatDefs/Post/Upsample.j3md");
        inject = new InjectionPass(assetManager, allocator);
        inject.getSceneColor().setUpstream(color);
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        if (position >= QUEUING) {
            return;
        }
        int passes = numSamplingSteps.preview();
        while (downsamples.size() < passes) {
            BlurPass blur = new BlurPass(allocator, downsampleMat, true);
            blur.bilinear.setUpstream(bilinear);
            if (!downsamples.isEmpty()) {
                blur.color.setUpstream(downsamples.getLast().getFilterResult());
            }
            downsamples.add(blur);
        }
        while (downsamples.size() > passes) {
            downsamples.removeLast();
        }
        while (upsamples.size() < passes) {
            BlurPass blur = new BlurPass(allocator, upsampleMat, false);
            blur.bilinear.setUpstream(bilinear);
            if (!upsamples.isEmpty()) {
                blur.color.setUpstream(upsamples.getLast().getFilterResult());
            }
            upsamples.add(blur);
        }
        while (upsamples.size() > passes) {
            upsamples.removeLast();
        }
        if (passes != 0) {
            downsamples.getFirst().color.setUpstream(color);
            upsamples.getFirst().color.setUpstream(downsamples.getLast().getFilterResult());
            inject.glow.setUpstream(upsamples.getLast().getFilterResult());
            result.setUpstream(inject.getFilterResult());
        } else {
            result.setUpstream(color);
        }
        super.preStage(globals);
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

    public ArgumentMacro<Integer> getNumSamplingSteps() {
        return numSamplingSteps;
    }

    public ArgumentSocket<Boolean> getBilinear() {
        return bilinear;
    }

    public ArgumentSocket<Float> getFactor() {
        return inject.factor;
    }

    private static class BlurPass extends RenderTask implements PostProcessFilter {

        private final boolean downsample;
        private final Material material;
        private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
        private final OptionalSocket<Texture2D> depth = new OptionalSocket<>(this, false);
        private final ArgumentSocket<Boolean> bilinear = new ArgumentSocket<>(this);
        private final AllocationSocket<Texture2D> result;
        private final AllocationSocket<FrameBuffer> frameBuffer;
        private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
        private final FrameBufferDef bufferDef = new FrameBufferDef();
        private final Vector2f texelSize = new Vector2f();
        private final CameraState camera = new CameraState(new Camera(1024, 1024), true);

        public BlurPass(ResourceAllocator allocator, Material material, boolean downsample) {
            this.downsample = downsample;
            this.material = material;
            addSockets(color, bilinear);
            result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
            frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        }

        @Override
        protected void renderTask() {
            Texture2D colorTex = color.acquireOrThrow("Scene color required.");
            Image colorImg = colorTex.getImage();
            if (downsample && (colorImg.getWidth() <= 2 || colorImg.getHeight() <= 2)) {
                throw new IllegalStateException("Texture too small to downsample.");
            }
            resultDef.setSize(nextSize(colorImg.getWidth()), nextSize(colorImg.getHeight()));
            resultDef.setSamples(colorImg.getMultiSamples());
            resultDef.setFormat(colorImg.getFormat());
            resultDef.setMinFilter(bilinear.acquireOrThrow() ? Texture.MinFilter.BilinearNoMipMaps : Texture.MinFilter.NearestNoMipMaps);
            camera.resize(resultDef.getWidth(), resultDef.getHeight(), false);
            context.getCamera().pushValue(camera);
            bufferDef.setColorTarget(result.acquire());
            FrameBuffer fbo = frameBuffer.acquire();
            context.getFrameBuffer().pushValue(fbo);
            context.clearBuffers();
            material.setTexture("Texture", colorTex);
            material.setVector2("TexelSize", texelSize.set(1f / colorImg.getWidth(), 1f / colorImg.getHeight()));
            if (colorImg.getMultiSamples() > 1) {
                material.setInt("NumSamples", colorImg.getMultiSamples());
            } else {
                material.clearParam("NumSamples");
            }
            context.renderFullscreen(material);
            context.getFrameBuffer().pop();
            context.getCamera().pop();
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

        private int nextSize(int n) {
            if (downsample) {
                return n >> 1;
            } else {
                return n << 1;
            }
        }

    }

    private static class InjectionPass extends AbstractFilterTask {

        private final TransitiveSocket<Texture2D> glow = new TransitiveSocket<>(this);
        private final ArgumentSocket<Float> factor = new ArgumentSocket<>(this, 0.05f);

        public InjectionPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator, new Material(assetManager, "Common/MatDefs/Post/SoftBloomFinal.j3md"), false);
            addSockets(glow, factor);
        }

        @Override
        protected void configureMaterial(Material material) {
            glow.acquireToMaterial(material, "GlowMap");
            factor.acquireToMaterial(material, "GlowFactor");
        }

    }

}
