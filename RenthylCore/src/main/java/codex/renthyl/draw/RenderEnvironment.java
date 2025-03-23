package codex.renthyl.draw;

import codex.boost.render.DepthRange;
import codex.renthyl.FGRenderContext;
import com.jme3.light.LightFilter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Contains parameters used for rendering and determines how
 * such parameters are merged when multiple parameters are available.
 */
public class RenderEnvironment {

    private String technique;
    private Material material;
    private RenderState state;
    private Predicate<Geometry> geometryFilter;
    private LightFilter lightFilter;
    private final ColorRGBA background = new ColorRGBA(0, 0, 0, 0);
    private DepthRange depthRange = new DepthRange(0, 1);
    private boolean parallelProjection;
    private Camera camera;

    public void apply(FGRenderContext context) {
        RenderManager rm = context.getRenderManager();
        Renderer r = rm.getRenderer();
        rm.setForcedTechnique(technique);
        rm.setForcedMaterial(material);
        rm.setForcedRenderState(state);
        rm.setRenderFilter(geometryFilter);
        rm.setLightFilter(lightFilter);
        r.setBackgroundColor(background);
        depthRange.apply(r);
        rm.setCamera(Objects.requireNonNull(camera, "Camera cannot be null."), parallelProjection);
    }

    public void fromViewPort(ViewPort vp) {
        background.set(vp.getBackgroundColor());
        camera = vp.getCamera();
    }
    public void fromRenderManager(RenderManager rm) {
        technique = rm.getForcedTechnique();
        material = rm.getForcedMaterial();
        RenderState forcedState = rm.getForcedRenderState();
        if (forcedState == null) {
            state = null;
        } else if (state == null) {
            state = new RenderState().copyFrom(forcedState);
        } else {
            state.copyFrom(forcedState);
        }
        geometryFilter = rm.getRenderFilter();
        lightFilter = rm.getLightFilter();
        camera = rm.getCurrentCamera();
    }

    public void copyFrom(RenderEnvironment env, boolean copyCamera) {
        technique = env.technique;
        material = env.material;
        if (env.state == null) {
            state = null;
        } else if (state != null) {
            state.copyFrom(env.state);
        } else {
            state = new RenderState().copyFrom(env.state);
        }
        geometryFilter = env.geometryFilter;
        lightFilter = env.lightFilter;
        background.set(env.background);
        depthRange.set(env.depthRange);
        parallelProjection = env.parallelProjection;
        if (!copyCamera || env.camera == null) {
            camera = env.camera;
        } else if (camera == null) {
            camera = env.camera.clone();
        } else {
            camera.copyFrom(env.camera);
        }
    }
    public void copyCamera(Camera camera) {
        if (this.camera == null) {
            this.camera = camera.clone();
        } else {
            this.camera.copyFrom(camera);
        }
    }
    public void copyCameraTransform(Camera camera) {
        if (this.camera == null) {
            this.camera = camera.clone();
        } else {
            this.camera.setLocation(camera.getLocation());
            this.camera.setRotation(camera.getRotation());
        }
    }

    public void setTechnique(String technique) {
        this.technique = technique;
    }
    public void setMaterial(Material material) {
        this.material = material;
    }
    public void setState(RenderState state) {
        this.state = state;
    }
    public void setGeometryFilter(Predicate<Geometry> geometryFilter) {
        this.geometryFilter = geometryFilter;
    }
    public void setLightFilter(LightFilter lightFilter) {
        this.lightFilter = lightFilter;
    }
    public void setDepthRange(DepthRange range) {
        this.depthRange.set(range);
    }
    public void setParallelProjection(boolean parallelProjection) {
        this.parallelProjection = parallelProjection;
    }
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public String getTechnique() {
        return technique;
    }
    public Material getMaterial() {
        return material;
    }
    public RenderState getState() {
        return state;
    }
    public Predicate<Geometry> getGeometryFilter() {
        return geometryFilter;
    }
    public LightFilter getLightFilter() {
        return lightFilter;
    }
    public ColorRGBA getBackground() {
        return background;
    }
    public DepthRange getDepthRange() {
        return depthRange;
    }
    public boolean isParallelProjection() {
        return parallelProjection;
    }
    public Camera getCamera() {
        return camera;
    }

}
