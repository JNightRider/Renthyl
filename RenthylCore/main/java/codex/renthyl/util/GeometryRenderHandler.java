/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.util;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

/**
 *
 * @author codex
 */
public interface GeometryRenderHandler {
    
    /**
     * Default implementation that renders the geometry and returns true.
     */
    GeometryRenderHandler DEFAULT = (rm, geom) -> {
        rm.renderGeometry(geom);
        return true;
    };
    
    /**
     * Renders the given geometry, or returns false.
     * 
     * @param rm render manager
     * @param g geometry to render
     * @return true if the geometry was rendered
     */
    public boolean renderGeometry(RenderManager rm, Geometry g);
    
}
