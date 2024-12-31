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
package codex.renthyl.util;

import codex.renthyl.FGRenderContext;
import codex.renthyl.Visibility;
import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

/**
 * Interface for handling rendering aspects, especially for rendering
 * of geometry and visibility checking.
 * 
 * @author codex
 */
public interface GeometryRenderHandler {
    
    GeometryRenderHandler DEFAULT = (context, geom) -> context.getRenderManager().renderGeometry(geom);
    
    /**
     * Renders the given geometry.
     * 
     * @param context
     * @param geometry geometry to render
     */
    public void renderGeometry(FGRenderContext context, Geometry geometry);
    
    /**
     * Returns the visibility status of the volume.
     * 
     * @param context
     * @param spatial
     * @param parent
     * @param gui
     * @return 
     */
    public default Visibility evaluateSpatialVisibility(FGRenderContext context, Spatial spatial, Visibility parent, boolean gui) {
        /*
         * Copyright (c) 2009-2021 jMonkeyEngine
         * All rights reserved.
         */
        Spatial.CullHint cm = spatial.getCullHint();
        assert cm != Spatial.CullHint.Inherit : "CullHint should never be inherit. Problem spatial name: " + spatial.getName();
        if (cm == Spatial.CullHint.Always) {
            return Visibility.OutsidePartial;
        } else if (cm == Spatial.CullHint.Never) {
            return Visibility.InsidePartial;
        } else if (parent.isPartial()) {
            return evaluateVolumeVisibility(context, spatial.getWorldBound(), gui);
        } else {
            return parent;
        }
    }
    
    /**
     * Returns the visibility status of the volume.
     * 
     * @param context
     * @param volume
     * @param gui
     * @return 
     */
    public default Visibility evaluateVolumeVisibility(FGRenderContext context, BoundingVolume volume, boolean gui) {
        if (!gui) {
            return Visibility.fromFrustumIntersect(context.getCurrentCamera().contains(volume));
        } else {
            return Visibility.get(context.getCurrentCamera().containsGui(volume), false);
        }
    }
    
}
