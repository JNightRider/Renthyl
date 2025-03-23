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
    
    private final FrameGraph frameGraph;
    private RenderManager renderManager;
    private FGPipelineContext context;
    private ViewPort viewPort;
    private float tpf;
    private final FullScreenQuad screen;
    private boolean temporalCulling = true;
    
    private final ArrayDeque<RenderMode> activeModes = new ArrayDeque<>();
    private final DepthRange depth = new DepthRange();
    private final RenderEnvironment baseEnv = new RenderEnvironment();

    /**
     * 
     * @param frameGraph
     */
    public FGRenderContext(FrameGraph frameGraph) {
        this.frameGraph = frameGraph;
        this.screen = new FullScreenQuad(this.frameGraph.getAssetManager());
    }
    
    /**
     * Targets this context to the viewport.
     * 
     * @param rm
     * @param context
     * @param vp
     * @param tpf 
     */
    public void target(RenderManager rm, FGPipelineContext context, ViewPort vp, float tpf) {
        this.renderManager = rm;
        this.context = context;
        this.viewPort = vp;
        this.tpf = tpf;
        if (this.viewPort == null) {
            throw new NullPointerException("ViewPort cannot be null.");
        }
        baseEnv.fromRenderManager(this.renderManager);
        baseEnv.fromViewPort(this.viewPort);
    }
    /**
     * Returns true if the context is ready for rendering.
     *
     * @return
     */
    public boolean isReady() {
        return renderManager != null && viewPort != null;
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
    
    /**
     * Applies saved render settings, except the framebuffer.
     */
    public void popActiveModes() {
        for (RenderMode m : activeModes) {
            m.reset(this);
        }
        activeModes.clear();
    }
    
    /**
     * Renders the material on a fullscreen quad.
     * 
     * @param mat 
     */
    public void renderFullscreen(Material mat) {
        screen.render(renderManager, mat);
    }
    /**
     * Renders the color and depth textures on a fullscreen quad, where
     * the color texture informs the color, and the depth texture informs
     * the depth.
     * <p>
     * If both color and depth are null, no rendering will be performed
     * 
     * @param color color texture, or null
     * @param depth depth texture, or null
     */
    public void renderTextures(Texture2D color, Texture2D depth) {
        screen.render(renderManager, color, depth);
    }
    
    /**
     * Renders the geometries mesh.
     * 
     * @param renderer
     * @param geometry 
     */
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
    
    /**
     * 
     * @param cam 
     */
    public void setCamera(Camera cam) {
        renderManager.setCamera(cam, cam.isParallelProjection());
    }
    /**
     * Updates the RenderManager with the given camera's properties only if
     * the given camera is the camera currently being used for rendering.
     * 
     * @param cam 
     */
    public void updateCamera(Camera cam) {
        if (cam == renderManager.getCurrentCamera()) {
            renderManager.setCamera(cam, cam.isParallelProjection());
        }
    }
    
    public void setDepth(DepthRange depth) {
        this.depth.set(depth);
        this.depth.apply(getRenderer());
    }

    /**
     * Enables culling based on culling results from the previous frame.
     * <p>
     * If not enabled, each module in the graph must be prepared and re-culled every frame
     * regardless of whether it will actually be executed. Temporal culling allows those
     * processes to be skipped if no layout update has been raised.
     *
     * @param temporalCulling 
     */
    protected void setTemporalCulling(boolean temporalCulling) {
        this.temporalCulling = temporalCulling;
    }
    
    /**
     * 
     * @return 
     */
    public FrameGraph getFrameGraph() {
        return frameGraph;
    }
    /**
     * Gets the resource list belonging to the framegraph.
     * 
     * @return 
     */
    public ResourceList getResources() {
        return frameGraph.getResources();
    }
    /**
     * Gets the render manager.
     * 
     * @return 
     */
    public RenderManager getRenderManager() {
        return renderManager;
    }
    /**
     * Gets the context for the FrameGraph pipeline.
     * 
     * @return 
     */
    public FGPipelineContext getPipelineContext() {
        return context;
    }
    /**
     * Gets the viewport currently being rendered.
     * 
     * @return 
     */
    public ViewPort getViewPort() {
        return viewPort;
    }
    /**
     * 
     * @return 
     */
    public Camera getCurrentCamera() {
        return renderManager.getCurrentCamera();
    }
    /**
     * Gets the profiler.
     * 
     * @return app profiler, or null
     */
    public AppProfiler getProfiler() {
        return renderManager.getProfiler();
    }
    /**
     * Gets the renderer held by the render manager.
     * 
     * @return 
     */
    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }
    /**
     * Gets the render queue held by the viewport.
     * 
     * @return 
     */
    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }
    /**
     * Gets the fullscreen quad used for fullscreen renders.
     * 
     * @return 
     */
    public FullScreenQuad getScreen() {
        return screen;
    }
    /**
     * Gets the current depth range.
     * 
     * @param store
     * @return 
     */
    public DepthRange getDepth(DepthRange store) {
        if (store == null) {
            store = new DepthRange();
        }
        return store.set(depth);
    }
    /**
     * Gets the time per frame.
     * 
     * @return 
     */
    public float getTpf() {
        return tpf;
    }
    /**
     * Gets the camera width.
     * 
     * @return 
     */
    public int getWidth() {
        return viewPort.getCamera().getWidth();
    }
    /**
     * Gets the camera height.
     * 
     * @return 
     */
    public int getHeight() {
        return viewPort.getCamera().getHeight();
    }
    /**
     * Returns true if the FrameGraph is asynchronous.
     * 
     * @return 
     */
    public boolean isAsync() {
        return frameGraph.isAsync();
    }
    /**
     * Returns true if temporal culling is enabled.
     * <p>
     * Temporal culling culls modules that were culled last frame
     * from being considered this frame. This assumes that the layout
     * of the FrameGraph has not changed.
     * <p>
     * Temporal culling can be enabled or disabled
     * 
     * @return 
     */
    public boolean isTemporalCulling() {
        return temporalCulling;
    }
    
    /**
     * Returns true if the app profiler is not null.
     * 
     * @return 
     */
    public boolean isProfilerAvailable() {
        return renderManager.getProfiler() != null;
    }
    
}
