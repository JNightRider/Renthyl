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
import codex.renthyl.draw.RenderEnvironment;
import codex.renthyl.resources.ResourceList;
import codex.renthyl.util.FullScreenQuad;
import codex.renthyl.draw.RenderMode;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.opencl.CommandQueue;
import com.jme3.opencl.Context;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.Texture2D;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedGeometry;
import java.util.ArrayDeque;

/**
 * Context for FrameGraph rendering.
 * <p>
 * Provides RenderPasses with access to important objects such as the RenderManager,
 * ViewPort, profiler, and a fullscreen quad. Utility methods are provided for
 * fullscreen quad rendering and camera management.
 * 
 * @author codex
 */
public class FGRenderContext {

    private final AssetManager assetManager;
    private RenderManager renderManager;
    private FGPipelineContext context;
    private ViewPort viewPort;
    private int width, height;
    private float tpf;
    private final FullScreenQuad screen;
    
    private final ArrayDeque<RenderMode> activeModes = new ArrayDeque<>();
    private final DepthRange depth = new DepthRange();
    private final RenderEnvironment baseEnv = new RenderEnvironment();

    public FGRenderContext(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.screen = new FullScreenQuad(assetManager);
    }

    public void target(RenderManager rm, FGPipelineContext context, ViewPort vp, float tpf) {
        this.renderManager = rm;
        this.context = context;
        this.viewPort = vp;
        this.tpf = tpf;
        Renderer r = getRenderer();
        r.setFrameBuffer(vp.getOutputFrameBuffer());
        this.renderManager.setCamera(vp.getCamera(), false);
        if (viewPort.isClearDepth() || viewPort.isClearColor() || viewPort.isClearStencil()) {
            if (viewPort.isClearColor()) {
                r.setBackgroundColor(viewPort.getBackgroundColor());
            }
            r.clearBuffers(viewPort.isClearColor(), viewPort.isClearDepth(), viewPort.isClearStencil());
        }
        width = this.viewPort.getCamera().getWidth();
        height = this.viewPort.getCamera().getHeight();
        baseEnv.fromRenderManager(this.renderManager);
        baseEnv.fromViewPort(this.viewPort);
    }
    
    public void registerMode(RenderMode mode) {
        activeModes.addFirst(mode);
        mode.apply(this);
    }

    public void clearBuffers() {
        clearBuffers(true, true, true);
    }

    public void clearBuffers(boolean color, boolean depth, boolean stencil) {
        getRenderer().clearBuffers(color, depth, stencil);
    }

    public void popActiveModes() {
        for (RenderMode m : activeModes) {
            m.reset(this);
        }
        activeModes.clear();
    }

    public void renderFullscreen(Material mat) {
        screen.render(renderManager, mat);
    }

    public void renderTextures(Texture2D color, Texture2D depth) {
        screen.render(renderManager, color, depth);
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

    public void setCamera(Camera cam) {
        renderManager.setCamera(cam, cam.isParallelProjection());
    }

    public void updateCamera(Camera cam) {
        if (cam == renderManager.getCurrentCamera()) {
            renderManager.setCamera(cam, cam.isParallelProjection());
        }
    }
    
    public void setDepth(DepthRange depth) {
        this.depth.set(depth);
        this.depth.apply(getRenderer());
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public FGPipelineContext getPipelineContext() {
        return context;
    }

    public ViewPort getViewPort() {
        return viewPort;
    }

    public Camera getCurrentCamera() {
        return renderManager.getCurrentCamera();
    }

    public AppProfiler getProfiler() {
        return renderManager.getProfiler();
    }

    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }

    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }

    public FullScreenQuad getScreen() {
        return screen;
    }

    public DepthRange getDepth(DepthRange store) {
        if (store == null) {
            store = new DepthRange();
        }
        return store.set(depth);
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
    
}
