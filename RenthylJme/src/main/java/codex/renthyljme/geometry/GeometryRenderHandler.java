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
package codex.renthyljme.geometry;

import codex.renthyljme.FrameGraphContext;
import codex.renthyl.render.CameraState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

/**
 * Handles rendering of geometries.
 * 
 * @author codex
 */
public interface GeometryRenderHandler {

    /**
     * Default handler which uses {@link com.jme3.renderer.RenderManager#renderGeometry(Geometry)} to render
     * the geometry. Spatial culling evaluation is unchanged.
     */
    GeometryRenderHandler DEFAULT = (context, geom) -> context.getRenderManager().renderGeometry(geom);
    
    /**
     * Renders the geometry.
     * 
     * @param context
     * @param geometry geometry to render
     */
    void renderGeometry(FrameGraphContext context, Geometry geometry);

    /**
     * Evaluates the {@link Visibility} of the spatial in relation to the camera.
     *
     * @param camera
     * @param spatial
     * @return
     */
    default Visibility evaluateSpatialCulling(CameraState camera, Spatial spatial) {
        return camera.visible(spatial.getWorldBound());
    }
    
}
