/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.draw;

import codex.renthyl.FGRenderContext;
import com.jme3.scene.Geometry;
import com.jme3.scene.control.Control;

/**
 *
 * @author codex
 */
public interface RenderControl extends Control {
    
    /**
     * 
     * @param context 
     */
    public void render(FGRenderContext context);
    
    public static void render(FGRenderContext context, Geometry geometry) {
        RenderControl c = geometry.getControl(RenderControl.class);
        if (c != null) {
            c.render(context);
        } else {
            context.getRenderManager().renderGeometry(geometry);
        }
    }
    
}
