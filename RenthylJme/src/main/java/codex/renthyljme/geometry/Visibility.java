/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.geometry;

import com.jme3.renderer.Camera;

/**
 * Spatial visibility states.
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

    /**
     * Returns true if the enumeration indicates that an object is inside the view.
     *
     * @return
     */
    public boolean isInside() {
        return inside;
    }

    /**
     * Returns true if the enumeration cannot be directly applied to descendents of the object.
     *
     * @return
     */
    public boolean isPartial() {
        return partial;
    }

    /**
     * Gets the corresponding {@link com.jme3.renderer.Camera.FrustumIntersect frustum intersect} enumeration
     * of this enumeration.
     *
     * @return
     */
    public Camera.FrustumIntersect getFrustumIntersect() {
        return intersect;
    }

    /**
     * Gets the Visibility enumeration corresponding to the properties.
     *
     * @param inside inside property
     * @param partial partial property
     * @return corresponding enum
     */
    public static Visibility get(boolean inside, boolean partial) {
        if (!inside && !partial) return Visibility.Outside;
        else if (!inside) return Visibility.OutsidePartial;
        else if (!partial) return Visibility.Inside;
        else return Visibility.InsidePartial;
    }

    /**
     * Returns the corresponding enumeration for the {@link com.jme3.renderer.Camera.FrustumIntersect intersection}
     * enumeration.
     *
     * @param intersect
     * @return corresponding Visibility enum
     * @throws UnsupportedOperationException if {@code intersect} has no corresponding Visibility enum
     */
    public static Visibility fromFrustumIntersect(Camera.FrustumIntersect intersect) {
        switch (intersect) {
            case Outside: return Visibility.Outside;
            case Intersects: return Visibility.InsidePartial;
            case Inside: return Visibility.Inside;
            default: throw new UnsupportedOperationException("Unrecognized " + Camera.FrustumIntersect.class.getName() + " enum constant: " + intersect);
        }
    }
    
}
