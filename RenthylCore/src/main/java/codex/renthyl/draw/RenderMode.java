/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.draw;

import codex.boost.render.DepthRange;
import codex.renthyl.FGRenderContext;
import com.jme3.light.LightFilter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import java.awt.Point;
import java.util.function.Predicate;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class RenderMode <T> {
    
    protected T targetValue;
    protected T savedValue;

    public RenderMode() {}
    public RenderMode(T targetValue) {
        this.targetValue = targetValue;
    }
    
    public void apply(FGRenderContext context) {
        this.savedValue = getCurrentValue(context);
        applyValue(context, targetValue);
    }
    public void reset(FGRenderContext context) {
        applyValue(context, savedValue);
    }
    
    protected abstract T getCurrentValue(FGRenderContext context);
    protected abstract void applyValue(FGRenderContext context, T value);
    
    public RenderMode<T> setTargetValue(T value) {
        this.targetValue = value;
        return this;
    }
    
    public T getTargetValue() {
        return targetValue;
    }
    public T getSavedValue() {
        return savedValue;
    }
    
    public static RenderMode<String> forcedTechnique(String technique) {
        return new ForcedTechniqueMode().setTargetValue(technique);
    }
    public static RenderMode<Material> forcedMaterial(Material mat) {
        return new ForcedMaterialMode().setTargetValue(mat);
    }
    public static RenderMode<FrameBuffer> frameBuffer(FrameBuffer fb) {
        return new FrameBufferMode().setTargetValue(fb);
    }
    public static RenderMode<Predicate<Geometry>> geometryFilter(Predicate<Geometry> filter) {
        return new GeometryFilterMode().setTargetValue(filter);
    }
    public static RenderMode<RenderState> forcedRenderState(RenderState state) {
        return new ForcedRenderStateMode().setTargetValue(state);
    }
    public static RenderMode<LightFilter> lightFilter(LightFilter filter) {
        return new LightFilterMode().setTargetValue(filter);
    }
    public static RenderMode<ColorRGBA> background(ColorRGBA background) {
        return new BackgroundMode().setTargetValue(background);
    }
    public static RenderMode<Camera> camera(Camera cam) {
        return new CameraMode().setTargetValue(cam);
    }
    public static RenderMode<Point> cameraSize(Point size) {
        return new CameraSizeMode().setTargetValue(size);
    }
    public static RenderMode<Point> cameraSize(int w, int h) {
        return new CameraSizeMode().setTargetValue(new Point(w, h));
    }
    public static RenderMode<Point> cameraSize(Point size, boolean fixAspect) {
        return new CameraSizeMode(fixAspect).setTargetValue(size);
    }
    public static RenderMode<Point> cameraSize(int w, int h, boolean fixAspect) {
        return new CameraSizeMode(fixAspect).setTargetValue(new Point(w, h));
    }
    public static RenderMode<Vector4f> cameraViewPort(Vector4f viewPort) {
        return new CameraViewPortMode().setTargetValue(viewPort);
    }
    public static RenderMode<Vector4f> cameraViewPort(float left, float right, float bottom, float top) {
        return new CameraViewPortMode(left, right, bottom, top);
    }
    public static RenderMode<Boolean> parallelProjection(boolean parallel) {
        return new ParallelProjectionMode().setTargetValue(parallel);
    }
    public static RenderMode<DepthRange> depthRange(DepthRange range) {
        return new DepthRangeMode().setTargetValue(range);
    }
    
    public static class ForcedTechniqueMode extends RenderMode<String> {

        @Override
        protected String getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getForcedTechnique();
        }

        @Override
        protected void applyValue(FGRenderContext context, String value) {
            context.getRenderManager().setForcedTechnique(value);
        }
        
    }
    public static class ForcedMaterialMode extends RenderMode<Material> {

        @Override
        protected Material getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getForcedMaterial();
        }
        
        @Override
        protected void applyValue(FGRenderContext context, Material value) {
            context.getRenderManager().setForcedMaterial(value);
        }
        
    }
    public static class FrameBufferMode extends RenderMode<FrameBuffer> {
        
        @Override
        protected FrameBuffer getCurrentValue(FGRenderContext context) {
            return context.getRenderer().getCurrentFrameBuffer();
        }

        @Override
        protected void applyValue(FGRenderContext context, FrameBuffer value) {
            context.getRenderer().setFrameBuffer(value);
        }
        
    }
    public static class GeometryFilterMode extends RenderMode<Predicate<Geometry>> {

        @Override
        protected Predicate<Geometry> getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getRenderFilter();
        }

        @Override
        protected void applyValue(FGRenderContext context, Predicate<Geometry> value) {
            context.getRenderManager().setRenderFilter(value);
        }
        
    }
    public static class ForcedRenderStateMode extends RenderMode<RenderState> {

        @Override
        protected RenderState getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getForcedRenderState();
        }

        @Override
        protected void applyValue(FGRenderContext context, RenderState value) {
            context.getRenderManager().setForcedRenderState(value);
        }
        
    }
    public static class LightFilterMode extends RenderMode<LightFilter> {

        @Override
        protected LightFilter getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getLightFilter();
        }

        @Override
        protected void applyValue(FGRenderContext context, LightFilter value) {
            context.getRenderManager().setLightFilter(value);
        }
        
    }
    public static class BackgroundMode extends RenderMode<ColorRGBA> {

        @Override
        protected ColorRGBA getCurrentValue(FGRenderContext context) {
            return context.getViewPort().getBackgroundColor();
        }

        @Override
        protected void applyValue(FGRenderContext context, ColorRGBA value) {
            context.getViewPort().setBackgroundColor(value);
            context.getRenderer().setBackgroundColor(value);
        }
        
    }
    public static class CameraMode extends RenderMode<Camera> {

        @Override
        protected Camera getCurrentValue(FGRenderContext context) {
            return context.getRenderManager().getCurrentCamera();
        }

        @Override
        protected void applyValue(FGRenderContext context, Camera value) {
            context.getRenderManager().setCamera(value, value.isParallelProjection());
        }
        
    }
    public static class CameraSizeMode extends RenderMode<Point> {
        
        private Camera cam;
        private final boolean fixAspect;

        public CameraSizeMode() {
            this(false);
        }
        public CameraSizeMode(boolean fixAspect) {
            this.fixAspect = fixAspect;
        }
        
        @Override
        protected Point getCurrentValue(FGRenderContext context) {
            cam = context.getRenderManager().getCurrentCamera();
            Point point = new Point();
            point.x = cam.getWidth();
            point.y = cam.getHeight();
            return point;
        }

        @Override
        protected void applyValue(FGRenderContext context, Point value) {
            if (cam.getWidth() != value.x || cam.getHeight() != cam.getHeight()) {
                cam.resize(value.x, value.y, fixAspect);
                context.updateCamera(cam);
            }
        }
        
    }
    public static class CameraViewPortMode extends RenderMode<Vector4f> {
        
        private Camera cam;
        
        public CameraViewPortMode() {}
        public CameraViewPortMode(float left, float right, float bottom, float top) {
            super(new Vector4f(left, right, bottom, top));
        }
        
        @Override
        protected Vector4f getCurrentValue(FGRenderContext context) {
            cam = context.getRenderManager().getCurrentCamera();
            return new Vector4f(cam.getViewPortLeft(), cam.getViewPortRight(), cam.getViewPortBottom(), cam.getViewPortTop());
        }

        @Override
        protected void applyValue(FGRenderContext context, Vector4f value) {
            if (value.x != cam.getViewPortLeft() || value.y != cam.getViewPortRight()
                    || value.z != cam.getViewPortBottom() || value.w != cam.getViewPortTop()) {
                cam.setViewPort(value.x, value.y, value.z, value.w);
                context.updateCamera(cam);
            }
        }
        
    }
    public static class ParallelProjectionMode extends RenderMode<Boolean> {
        
        private Camera cam;
        
        @Override
        protected Boolean getCurrentValue(FGRenderContext context) {
            cam = context.getRenderManager().getCurrentCamera();
            return cam.isParallelProjection();
        }
        
        @Override
        protected void applyValue(FGRenderContext context, Boolean value) {
            cam.setParallelProjection(value);
            context.updateCamera(cam);
        }
        
    }
    public static class DepthRangeMode extends RenderMode<DepthRange> {

        @Override
        protected DepthRange getCurrentValue(FGRenderContext context) {
            return context.getDepth(null);
        }

        @Override
        protected void applyValue(FGRenderContext context, DepthRange value) {
            context.setDepth(value);
        }
        
    }
    
}
