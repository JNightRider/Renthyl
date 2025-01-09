/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import com.jme3.bounding.BoundingVolume;
import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class GuiCamera extends Camera {

    protected GuiCamera() {}
    public GuiCamera(int width, int height) {
        super(width, height);
    }
    
    @Override
    public FrustumIntersect contains(BoundingVolume volume) {
        return containsGui(volume) ? FrustumIntersect.Inside : FrustumIntersect.Outside;
    }
    
}
