/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.GlobalAttributes;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.geometry.GeometryRenderHandler;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.util.TempVars;

import java.util.List;

/**
 *
 * @author gary
 * @param <T>
 */
public abstract class ShadowOcclusionPass <T extends Light> extends RenderTask implements GeometryRenderHandler {
    
    protected final Light.Type lightType;
    protected final int numShadowMaps;
    protected final ShadowMapDef shadowMapDef = new ShadowMapDef();
    private final ArgumentSocket<T> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new TransitiveSocket<>(this);
    private final SocketList<AllocationSocket<ShadowMap>, ShadowMap> shadowMaps = new SocketList<>(this);
    private final SocketList<AllocationSocket<FrameBuffer>, FrameBuffer> frameBuffers = new SocketList<>(this);
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final RenderState renderState = new RenderState();
    private final Material material;
    
    private float renderDistance = -1;

    public ShadowOcclusionPass(AssetManager assetManager, ResourceAllocator allocator, Light.Type lightType, int numShadowMaps, int shadowMapSize) {
        this.lightType = lightType;
        this.numShadowMaps = numShadowMaps;
        addSockets(light, occluders, receivers, shadowMaps, frameBuffers);
        for (int i = 0; i < numShadowMaps; i++) {
            shadowMaps.add(new AllocationSocket<>(this, allocator, shadowMapDef));
            frameBuffers.add(new AllocationSocket<>(this, allocator, bufferDef));
        }
        shadowMapDef.getMapDef().setSquare(shadowMapSize);
        shadowMapDef.getMapDef().setWrap(Texture.WrapMode.EdgeClamp);
        //renderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        renderState.setColorWrite(false);
        renderState.setDepthWrite(true);
        renderState.setDepthTest(true);
        //renderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        super.preStage(globals);
    }

    @Override
    protected void renderTask() {

        CameraState viewCam = context.getCamera().getValue();
        T lightSource = light.acquireOrThrow();

        // check if light influence intersects viewing camera
        TempVars vars = TempVars.get();
        boolean intersects = lightSource != null && lightSource.intersectsFrustum(viewCam.getCamera(), vars);
        vars.release();
        if (!intersects) {
            return;
        }

        boolean containsAll = lightSourceInsideFrustum(viewCam.getCamera(), lightSource);
        GeometryQueue occluderQueue = occluders.acquireOrThrow();
        GeometryQueue receiverQueue = receivers.acquireOrThrow();

        // apply settings
        context.getForcedState().pushValue(renderState);
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(material);
        context.getFrameBuffer().push();
        context.getCamera().push();

        // render each shadow map
        for (int i = 0; i < numShadowMaps; i++) {
            CameraState shadowCam = getShadowCamera(context, viewCam, occluderQueue, receiverQueue, lightSource, i);
            ShadowMap map = acquireShadowMap(shadowCam, lightSource, shadowMaps.get(i), i);
            bufferDef.setDepthTarget(map.getMap());
            FrameBuffer fbo = frameBuffers.get(i).acquire();
            context.getFrameBuffer().setValue(fbo);
            if (containsAll || frustumIntersect(viewCam.getCamera(), shadowCam.getCamera())) {
                context.getCamera().setValue(shadowCam);
                context.getFrameBuffer().setValue(fbo);
                context.clearBuffers(false, true, false);
                System.out.println("rendered " + occluderQueue.render(context, this) + " out of " + occluderQueue.size() + " occlusion geometries");
            }
        }

        // restore settings
        context.getFrameBuffer().pop();
        context.getForcedMaterial().pop();
        context.getForcedTechnique().pop();
        context.getForcedState().pop();
        context.getCamera().pop();

    }

    @Override
    public void renderGeometry(FrameGraphContext context, Geometry g) {
        context.getRenderManager().renderGeometry(g);
    }
    
    protected abstract boolean lightSourceInsideFrustum(Camera cam, T light);

    protected abstract CameraState getShadowCamera(FrameGraphContext context, CameraState viewCam, GeometryQueue occluders, GeometryQueue receivers, T light, int index);
    
    @SuppressWarnings("SameReturnValue")
    protected boolean frustumIntersect(Camera cam1, Camera cam2) {
        return true;
    }

    protected ShadowMap acquireShadowMap(CameraState cam, T light, Socket<ShadowMap> socket, int i) {
        ShadowMap map = socket.acquire();
        map.setLight(light);
        map.setProjection(cam.getCamera().getViewProjectionMatrix());
        map.setRange(cam.getCamera().getFrustumNear(), cam.getCamera().getFrustumFar());
        return map;
    }

    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }

    public float getRenderDistance() {
        return renderDistance;
    }

    public ArgumentSocket<T> getLight() {
        return light;
    }

    public PointerSocket<GeometryQueue> getOccluders() {
        return occluders;
    }

    public PointerSocket<GeometryQueue> getReceivers() {
        return receivers;
    }

    public Socket<List<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

    public static boolean pointInsideFrustum(Camera cam, Vector3f point) {
        for (int i = 0; i < 6; i++) {
            if (cam.getWorldPlane(i).whichSide(point) == Plane.Side.Negative) {
                return false;
            }
        }
        return true;
    }
    
}
