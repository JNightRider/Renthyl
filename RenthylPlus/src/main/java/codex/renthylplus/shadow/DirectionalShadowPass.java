/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.geometry.GeometryRenderHandler;
import codex.renthyl.geometry.Visibility;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class DirectionalShadowPass extends RenderTask implements Occlusion<DirectionalLight>, GeometryRenderHandler {

    private static final float MIN_FRUSTUM = .5f;

    private final int baseMapSize;
    private final ArgumentSocket<DirectionalLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new TransitiveSocket<>(this);
    private final ArgumentSocket<Float> maxDistance = new ArgumentSocket<>(this, 1000f);
    private final SocketList<ShadowMapSocket, ShadowMap> shadowMaps = new SocketList<>(this);
    private final CameraState camera = new CameraState(new Camera(1024, 1024), false);
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public DirectionalShadowPass(AssetManager assetManager, ResourceAllocator allocator, int shadowMapSize, int splits) {
        if ((shadowMapSize >> (splits - 1)) <= 1) {
            throw new IllegalArgumentException("Base shadow map size must be greater than 2^splits.");
        }
        this.baseMapSize = shadowMapSize;
        this.backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        addSockets(light, occluders, receivers, maxDistance, shadowMaps);
        for (int i = 0; i < splits; i++) {
            shadowMaps.add(new ShadowMapSocket(this, allocator));
        }
        camera.getCamera().setParallelProjection(true);
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }
    
    @Override
    protected void renderTask() {

        // setup context settings
        context.getCamera().push();
        context.getFrameBuffer().push();
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        DirectionalLight dl = light.acquireOrThrow("Light required.");

        // calculate frustum
        float nearFrustum = MIN_FRUSTUM;
        float farFrustum = maxDistance.acquireOrThrow("Maximum render distance required.");
        GeometryQueue occluderQueue = occluders.acquireOrThrow("Occluder geometry required.");
        GeometryQueue receiverQueue = receivers.acquireOrThrow("Receiver geometry required.");
        BoundingBox occluderBB = new BoundingBox();
        for (Geometry g : occluderQueue) {
            g.updateGeometricState();
            occluderBB.mergeLocal(g.getWorldBound());
        }
        float radius = positionCamera(dl, occluderBB, nearFrustum, farFrustum);
        farFrustum = Math.min(farFrustum, nearFrustum + radius * 2f);

        int size = baseMapSize;
        float splitLength = (farFrustum - nearFrustum) / shadowMaps.size();

        for (ShadowMapSocket socket : shadowMaps) {
            farFrustum = nearFrustum + splitLength;

            // configure camera
            camera.resize(size, size, false);
            camera.getCamera().setFrustumNear(nearFrustum);
            camera.getCamera().setFrustumFar(farFrustum);
            camera.getCamera().update();
            camera.getCamera().updateViewProjection();
            context.getCamera().setValue(camera);

            // configure shadow map
            socket.setSize(size, size);
            ShadowMap map = socket.getShadowMap().acquire();
            map.setLight(dl);
            map.setProjection(camera.getCamera().getViewProjectionMatrix());
            map.setRange(nearFrustum, farFrustum);

            // configure framebuffer
            socket.setTargetDepth(map.getMap());
            FrameBuffer fbo = socket.getFrameBuffer().acquire();
            context.getFrameBuffer().setValue(fbo);
            context.clearBuffers();

            // render
            occluderQueue.render(context);

            nearFrustum = farFrustum;
            size = size >> 1;
        }

        // restore context settings
        context.getCamera().pop();
        context.getFrameBuffer().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();

    }

    private float positionCamera(DirectionalLight dl, BoundingBox include, float near, float far) {
        float radius = include.getMax(null).subtractLocal(include.getCenter()).length();
        camera.getCamera().setLocation(include.getCenter().subtract(dl.getDirection().mult(radius + near)));
        camera.getCamera().lookAtDirection(dl.getDirection(), camera.getCamera().getUp());
        camera.getCamera().setFrustum(near, far, -radius, radius, radius, -radius);
        return radius;
    }

    @Override
    public ArgumentSocket<DirectionalLight> getLight() {
        return light;
    }

    @Override
    public PointerSocket<GeometryQueue> getOccluders() {
        return occluders;
    }

    @Override
    public PointerSocket<GeometryQueue> getReceivers() {
        return receivers;
    }

    @Override
    public ArgumentSocket<Float> getMaxDistance() {
        return maxDistance;
    }

    @Override
    public Socket<? extends Collection<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

    private Vector3f min(Vector3f v1, Vector3f v2, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.x = Math.min(v1.x, v2.x);
        store.y = Math.min(v1.y, v2.y);
        store.z = Math.min(v1.z, v2.z);
        return store;
    }

    private Vector3f max(Vector3f v1, Vector3f v2, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        store.x = Math.max(v1.x, v2.x);
        store.y = Math.max(v1.y, v2.y);
        store.z = Math.max(v1.z, v2.z);
        return store;
    }

    @Override
    public void renderGeometry(FrameGraphContext context, Geometry geometry) {
        context.getRenderManager().renderGeometry(geometry);
    }

    @Override
    public Visibility evaluateSpatialCulling(CameraState camera, Spatial spatial) {
        return Visibility.Inside;
    }

}
