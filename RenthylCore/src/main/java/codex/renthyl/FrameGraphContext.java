/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl;

import codex.boost.render.DepthRange;
import codex.renthyl.render.CameraState;
import codex.renthyl.render.ContextSetting;
import codex.renthyl.util.FullScreenQuad;
import com.jme3.asset.AssetManager;
import com.jme3.light.LightFilter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.pipeline.PipelineContext;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedGeometry;

import java.util.Objects;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Context for FrameGraph rendering.
 * <p>
 * Provides RenderPasses with access to important objects such as the RenderManager,
 * ViewPort, profiler, and a fullscreen quad. Utility methods are provided for
 * fullscreen quad rendering and camera management.
 * 
 * @author codex
 */
public class FrameGraphContext implements PipelineContext {

    private final AssetManager assetManager;
    private final RenderManager renderManager;
    private final FullScreenQuad screen;
    private ViewPort viewPort;
    private int width, height;
    private float tpf;
    private boolean rendered = false;

    private final RenderSetting<FrameBuffer> frameBuffer = new RenderSetting<>((rm, fbo) -> rm.getRenderer().setFrameBuffer(fbo), rm -> rm.getRenderer().getCurrentFrameBuffer());
    private final RenderSetting<String> forcedTechnique = new RenderSetting<>(RenderManager::setForcedTechnique, RenderManager::getForcedTechnique);
    private final RenderSetting<Material> forcedMaterial = new RenderSetting<>(RenderManager::setForcedMaterial, RenderManager::getForcedMaterial);
    private final RenderSetting<RenderState> forcedState = new RenderSetting<>(RenderManager::setForcedRenderState, RenderManager::getForcedRenderState);
    private final RenderSetting<Predicate<Geometry>> geometryFilter = new RenderSetting<>(RenderManager::setRenderFilter, RenderManager::getRenderFilter);
    private final RenderSetting<LightFilter> lightFilter = new RenderSetting<>(RenderManager::setLightFilter, RenderManager::getLightFilter);
    private final RenderSetting<DepthRange> depthRange = new RenderSetting<>((rm, d) -> d.apply(rm.getRenderer()), rm -> DepthRange.NORMAL);
    private final RenderSetting<Boolean> passDrawBufferId = new RenderSetting<>(RenderManager::setPassDrawBufferTargetIdToShaders, RenderManager::getPassDrawBufferTargetIdToShaders);
    private final RenderSetting<TechniqueDef.LightMode> lightMode = new RenderSetting<>(RenderManager::setPreferredLightMode, RenderManager::getPreferredLightMode);
    private final RenderSetting<Integer> lightBatchSize = new RenderSetting<>(RenderManager::setSinglePassLightBatchSize, RenderManager::getSinglePassLightBatchSize);
    private final CameraSetting camera = new CameraSetting();

    public FrameGraphContext(AssetManager assetManager, RenderManager renderManager) {
        this.assetManager = assetManager;
        this.renderManager = renderManager;
        this.screen = new FullScreenQuad(assetManager);
    }

    @Override
    public boolean startViewPortRender(RenderManager rm, ViewPort vp) {
        return rendered;
    }

    @Override
    public void endViewPortRender(RenderManager rm, ViewPort vp) {
        rendered = true;
    }

    @Override
    public void endContextRenderFrame(RenderManager rm) {}

    public void target(ViewPort vp, float tpf) {
        this.viewPort = vp;
        this.tpf = tpf;
        Renderer r = getRenderer();
        frameBuffer.setValue(vp.getOutputFrameBuffer());
        camera.setValue(viewPort.getCamera(), false);
        if (viewPort.isClearDepth() || viewPort.isClearColor() || viewPort.isClearStencil()) {
            if (viewPort.isClearColor()) {
                r.setBackgroundColor(viewPort.getBackgroundColor());
            }
            r.clearBuffers(viewPort.isClearColor(), viewPort.isClearDepth(), viewPort.isClearStencil());
        }
        width = viewPort.getCamera().getWidth();
        height = viewPort.getCamera().getHeight();
    }

    public void clearBuffers() {
        clearBuffers(true, true, true);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        getRenderer().clearBuffers(color, depth, stencil);
    }

    public void popAllRenderSettings() {
        frameBuffer.pop();
        forcedTechnique.pop();
        forcedMaterial.pop();
        forcedState.pop();
        geometryFilter.pop();
        lightFilter.pop();
        passDrawBufferId.pop();
        lightMode.pop();
        lightBatchSize.pop();
        camera.pop();
    }

    public void resetAllRenderSettings() {
        frameBuffer.reset();
        forcedTechnique.reset();
        forcedMaterial.reset();
        forcedState.reset();
        geometryFilter.reset();
        lightFilter.reset();
        passDrawBufferId.reset();
        lightMode.reset();
        lightBatchSize.reset();
        camera.reset();
    }

    public void renderFullscreen(Material mat) {
        screen.render(renderManager, mat);
    }

    public void renderTextures(Texture2D color, Texture2D depth) {
        screen.render(renderManager, color, depth);
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }

    public FullScreenQuad getScreen() {
        return screen;
    }

    public float getTpf() {
        return tpf;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public RenderSetting<FrameBuffer> getFrameBuffer() {
        return frameBuffer;
    }

    public RenderSetting<String> getForcedTechnique() {
        return forcedTechnique;
    }

    public RenderSetting<Material> getForcedMaterial() {
        return forcedMaterial;
    }

    public RenderSetting<RenderState> getForcedState() {
        return forcedState;
    }

    public RenderSetting<Predicate<Geometry>> getGeometryFilter() {
        return geometryFilter;
    }

    public RenderSetting<LightFilter> getLightFilter() {
        return lightFilter;
    }

    public RenderSetting<DepthRange> getDepthRange() {
        return depthRange;
    }

    public RenderSetting<Boolean> getPassDrawBufferId() {
        return passDrawBufferId;
    }

    public RenderSetting<TechniqueDef.LightMode> getLightMode() {
        return lightMode;
    }

    public RenderSetting<Integer> getLightBatchSize() {
        return lightBatchSize;
    }

    public CameraSetting getCamera() {
        return camera;
    }

    public static void renderMeshFromGeometry(Renderer renderer, Geometry geometry) {
        /*
         * Copyright (c) 2009-2024 jMonkeyEngine
         * All rights reserved.
         */
        Mesh mesh = geometry.getMesh();
        int lodLevel = geometry.getLodLevel();
        if (geometry instanceof InstancedGeometry) {
            InstancedGeometry instGeom = (InstancedGeometry) geometry;
            int numVisibleInstances = instGeom.getNumVisibleInstances();
            if (numVisibleInstances > 0) {
                renderer.renderMesh(mesh, lodLevel, numVisibleInstances, instGeom.getAllInstanceData());
            }
        } else {
            renderer.renderMesh(mesh, lodLevel, 1, null);
        }
    }

    public final class RenderSetting<T> implements ContextSetting<T> {

        private final BiConsumer<RenderManager, T> setter;
        private final Function<RenderManager, T> getter;
        private final Stack<T> values = new Stack<>();

        private RenderSetting(BiConsumer<RenderManager, T> setter, Function<RenderManager, T> getter) {
            this.setter = setter;
            this.getter = getter;
        }

        @Override
        public void push() {
            values.push(getter.apply(renderManager));
        }

        @Override
        public void setValue(T value) {
            setter.accept(renderManager, value);
        }

        @Override
        public T getValue() {
            return getter.apply(renderManager);
        }

        @Override
        public void pop() {
            if (!values.isEmpty()) {
                setter.accept(renderManager, values.pop());
            }
        }

        @Override
        public void reset() {
            if (!values.isEmpty()) {
                setValue(values.getFirst());
                values.clear();
            }
        }

    }

    public final class CameraSetting implements ContextSetting<CameraState> {

        private final Stack<CameraState> cameras = new Stack<>();
        private CameraState active;

        @Override
        public void push() {
            cameras.push(Objects.requireNonNullElseGet(active, () -> new CameraState(renderManager.getCurrentCamera(), false)));
        }

        @Override
        public void setValue(CameraState value) {
            (active = value).applyToContext(renderManager);
        }

        @Override
        public CameraState getValue() {
            return active;
        }

        @Override
        public void pop() {
            if (!cameras.isEmpty()) {
                (active = cameras.pop()).applyToContext(renderManager);
            }
        }

        @Override
        public void reset() {
            if (!cameras.isEmpty()) {
                (active = cameras.getFirst()).applyToContext(renderManager);
                cameras.clear();
            }
        }

        public void pushValue(Camera camera, boolean orthogonal) {
            pushValue(new CameraState(camera, orthogonal));
        }

        public void pushValue(boolean orthogonal) {
            pushValue(new CameraState(renderManager.getCurrentCamera(), orthogonal));
        }

        public void setValue(Camera camera, boolean orthogonal) {
            setValue(new CameraState(camera, orthogonal));
        }

        public void setValue(boolean orthogonal) {
            setValue(new CameraState(renderManager.getCurrentCamera(), orthogonal));
        }

        public Camera pushResize(Camera store, int width, int height, boolean fixAspect) {
            if (width != active.getCamera().getWidth() || height != active.getCamera().getHeight()) {
                if (store == null) {
                    store = active.getCamera().clone();
                } else {
                    store.copyFrom(active.getCamera());
                }
                pushValue(store, active.isOrthogonal());
                return store;
            }
            pushValue(active);
            return null;
        }

    }
    
}
