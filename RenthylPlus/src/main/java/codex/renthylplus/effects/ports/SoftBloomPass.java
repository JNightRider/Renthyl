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

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.render.CameraState;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.macros.ArgumentMacro;
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

public class SoftBloomPass extends RenderTask implements PostProcessFilter {

    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> depth = new OptionalSocket<>(this, false);
    private final ArgumentMacro<Integer> numSamplingSteps = new ArgumentMacro<>(5);
    private final ArgumentSocket<Boolean> bilinear = new ArgumentSocket<>(this, true);
    private final GenerativeSocketList<AllocationSocket<Texture2D>, Texture2D> targets;
    private final GenerativeSocketList<AllocationSocket<FrameBuffer>, FrameBuffer> frameBuffers;
    private final TextureDef<Texture2D> targetDef = TextureDef.texture2D();
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final Material downsampleMat, upsampleMat;
    private final ArrayList<CameraState> cameras = new ArrayList<>();
    private final Vector2f tempTexelSize = new Vector2f();
    private final InjectionPass inject;

    public SoftBloomPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(color, depth, numSamplingSteps, bilinear);
        targets = addSocket(new GenerativeSocketList<>(this, () -> new AllocationSocket<>(this, allocator, targetDef)));
        frameBuffers = addSocket(new GenerativeSocketList<>(this, () -> new AllocationSocket<>(this, allocator, bufferDef)));
        downsampleMat = new Material(assetManager, "Common/MatDefs/Post/Downsample.j3md");
        upsampleMat = new Material(assetManager,"Common/MatDefs/Post/Upsample.j3md");
        targets.fill(2);
        frameBuffers.fill(2);
        inject = new InjectionPass(assetManager, allocator);
        inject.getSceneColor().setUpstream(color);
        inject.glow.setUpstream(targets.getFirst());
    }

    @Override
    public void stage(RenderingQueue queue) {
        int passes = numSamplingSteps.preview() + 1;
        if (passes < 2) {
            throw new IllegalArgumentException("Must have at least one sampling pass.");
        }
        targets.set(passes);
        frameBuffers.set(passes);
        while (cameras.size() > passes) {
            cameras.removeLast();
        }
        super.stage(queue);
    }

    @Override
    protected void renderTask() {

        Texture2D source = color.acquireOrThrow("Scene color required.");
        int originWidth = source.getImage().getWidth();
        int originHeight = source.getImage().getHeight();
        targetDef.setFormat(source.getImage().getFormat());
        targetDef.setSamples(source.getImage().getMultiSamples());
        targetDef.setSize(originWidth >> 1, originHeight >> 1);
        targetDef.setMinFilter(bilinear.acquireOrThrow() ? Texture.MinFilter.BilinearNoMipMaps : Texture.MinFilter.NearestNoMipMaps);

        if (targetDef.getSamples() > 1) {
            downsampleMat.setInt("NumSamples", targetDef.getSamples());
            upsampleMat.setInt("NumSamples", targetDef.getSamples());
        } else {
            downsampleMat.clearParam("NumSamples");
            upsampleMat.clearParam("NumSamples");
        }

        context.getFrameBuffer().push();
        context.getCamera().push();
        configureCamera(0, originWidth, originHeight);

        // downsampling
        int i = 1;
        for (; i < targets.size(); i++) {
            context.getCamera().setValue(configureCamera(i, targetDef.getWidth(), targetDef.getHeight()));
            targetDef.setSize(source.getImage().getWidth() >> 1, source.getImage().getHeight() >> 1);
            Texture2D target = targets.get(i).acquire();
            // framebuffer
            bufferDef.setColorTarget(target);
            FrameBuffer fbo = frameBuffers.get(i).acquire();
            context.getFrameBuffer().setValue(fbo);
            context.clearBuffers();
            // material
            downsampleMat.setTexture("Texture", source);
            downsampleMat.setVector2("TexelSize", calcTexelSize(source.getImage(), tempTexelSize));
            // render
            context.renderFullscreen(downsampleMat);
            source = target;
            if (targetDef.getWidth() <= 2 || targetDef.getHeight() <= 2) {
                break;
            }
        }

        // upsampling
        for (i--; i >= 0; i--) {
            context.getCamera().setValue(cameras.get(i));
            targetDef.setSize(source.getImage().getWidth() << 1, source.getImage().getHeight() << 1);
            Texture2D target = targets.get(i).acquire();
            // framebuffer
            bufferDef.setColorTarget(target);
            FrameBuffer fbo = frameBuffers.get(i).acquire();
            context.getFrameBuffer().setValue(fbo);
            context.clearBuffers(true, false, false);
            // material
            upsampleMat.setTexture("Texture", source);
            upsampleMat.setVector2("TexelSize", calcTexelSize(source.getImage(), tempTexelSize));
            // render
            context.renderFullscreen(upsampleMat);
            source = target;
        }

        // reset settings
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
        return targets.getFirst();
    }

    private CameraState configureCamera(int i, int width, int height) {
        CameraState cam;
        if (i >= cameras.size()) {
            cam = new CameraState(new Camera(width, height), true);
            cameras.add(cam);
        } else {
            cam = cameras.get(i);
            cam.resize(width, height, false);
        }
        return cam;
    }

    private Vector2f calcTexelSize(Image sourceImage, Vector2f store) {
        if (store == null) {
            store = new Vector2f();
        }
        return store.set(1f / sourceImage.getWidth(), 1f / sourceImage.getHeight());
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
