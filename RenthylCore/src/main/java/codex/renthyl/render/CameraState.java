package codex.renthyl.render;

import codex.renthyl.geometry.Visibility;
import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;

/**
 * Camera wrapper containing an orthogonal property for {@link RenderManager#setCamera(Camera, boolean)}.
 *
 * @author codex
 */
public class CameraState {

    private final Camera camera;
    private final boolean orthogonal;

    public CameraState(Camera camera, boolean orthogonal) {
        this.camera = camera;
        this.orthogonal = orthogonal;
    }

    public void applyToContext(RenderManager rm) {
        rm.setCamera(camera, orthogonal);
    }

    public void resize(int width, int height, boolean fixAspect) {
        if (fixAspect || width != camera.getWidth() || height != camera.getHeight()) {
            camera.resize(width, height, fixAspect);
        }
    }

    public void copyFrom(CameraState camera) {
        this.camera.copyFrom(camera.getCamera());
    }

    public Visibility visible(BoundingVolume volume) {
        if (!orthogonal) {
            return Visibility.fromFrustumIntersect(camera.contains(volume));
        } else {
            return Visibility.get(camera.containsGui(volume), false);
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public boolean isOrthogonal() {
        return orthogonal;
    }

    public Vector3f getLocation() {
        return camera.getLocation();
    }

    public Quaternion getRotation() {
        return camera.getRotation();
    }

    public boolean identical(CameraState c) {
        return c != null && camera == c.getCamera() && orthogonal == c.isOrthogonal();
    }

}
