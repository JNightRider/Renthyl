/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.geometry;

import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public enum Visibility {
    
    /**
     * Indicates that an object and all its descendents are not visible.
     */
    Outside(false, false, Camera.FrustumIntersect.Outside),
    
    /**
     * Indicates that an object is not visible, but not necessarily its descendents.
     */
    OutsidePartial(false, true, Camera.FrustumIntersect.Intersects),
    
    /**
     * Indicates that an object is visible, but not necessarily its descendents.
     */
    InsidePartial(true, true, Camera.FrustumIntersect.Intersects),
    
    /**
     * Indicates that an object and all its descendents are visible.
     */
    Inside(true, false, Camera.FrustumIntersect.Inside);
    
    private final boolean inside;
    private final boolean partial;
    private final Camera.FrustumIntersect intersect;

    Visibility(boolean inside, boolean partial, Camera.FrustumIntersect intersect) {
        this.inside = inside;
        this.partial = partial;
        this.intersect = intersect;
    }
    
    public boolean isInside() {
        return inside;
    }

    public boolean isPartial() {
        return partial;
    }

    public Camera.FrustumIntersect getFrustumIntersect() {
        return intersect;
    }
    
    public static Visibility get(boolean inside, boolean partial) {
        if (!inside && !partial) return Visibility.Outside;
        else if (!inside) return Visibility.OutsidePartial;
        else if (!partial) return Visibility.Inside;
        else return Visibility.InsidePartial;
    }

    public static Visibility fromFrustumIntersect(Camera.FrustumIntersect intersect) {
        switch (intersect) {
            case Outside: return Visibility.Outside;
            case Intersects: return Visibility.InsidePartial;
            case Inside: return Visibility.Inside;
            default: throw new UnsupportedOperationException("Unrecognized " + Camera.FrustumIntersect.class.getName() + " enum constant: " + intersect);
        }
    }
    
}
