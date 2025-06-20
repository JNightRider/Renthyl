/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyljme.tasks.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class PointShadowPass extends RasterTask implements Occlusion<PointLight> {

    private static final Vector3f[] DIRECTIONS = {Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z,
            Vector3f.UNIT_X.negate(), Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z.negate()};
    private static final Vector3f[] UPS = {Vector3f.UNIT_Y, Vector3f.UNIT_Z, Vector3f.UNIT_Y,
            Vector3f.UNIT_Y, Vector3f.UNIT_Z, Vector3f.UNIT_Y};

    private final ArgumentSocket<PointLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new OptionalSocket<>(this, false);
    private final SocketList<ShadowMapSocket, ShadowMap> shadowMaps = new SocketList<>(this);
    private final CameraState[] cameras = new CameraState[DIRECTIONS.length];
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public PointShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        addSockets(light, occluders, receivers, shadowMaps);
        for (int i = 0; i < DIRECTIONS.length; i++) {
            shadowMaps.addSocket(new ShadowMapSocket(this, allocator)).setSize(size, size);
            Camera c = (cameras[i] = new CameraState(new Camera(size, size), false)).getCamera();
            c.lookAtDirection(DIRECTIONS[i], UPS[i]);
            c.setFrustumPerspective(90f, 1f, 1f, 2f);
        }
        backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }

    @Override
    protected void renderTask() {

        PointLight pl = light.acquireOrThrow("Light required.");

        context.getFrameBuffer().push();
        context.getCamera().push();
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        int i = 0;
        for (ShadowMapSocket socket : shadowMaps) {

            CameraState cam = cameras[i++];
            if (!cam.getCamera().getLocation().equals(pl.getPosition()) || cam.getCamera().getFrustumFar() != pl.getRadius()) {
                cam.getCamera().setLocation(pl.getPosition());
                cam.getCamera().setFrustumFar(pl.getRadius());
                cam.getCamera().update();
                cam.getCamera().updateViewProjection();
            }
            context.getCamera().setValue(cam);

            ShadowMap map = socket.acquire();
            map.setLight(pl);
            map.setProjection(cam.getCamera().getViewProjectionMatrix());
            map.setRange(cam.getCamera().getFrustumNear(), cam.getCamera().getFrustumFar());
            socket.setTargetDepth(map.getMap());

            FrameBuffer fbo = socket.getFrameBuffer().acquire();
            context.getFrameBuffer().setValue(fbo);
            context.clearBuffers();

            occluders.acquireOrThrow("Occluder queue required.").render(context);

        }

        context.getFrameBuffer().pop();
        context.getCamera().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();

    }

    @Override
    public ArgumentSocket<PointLight> getLight() {
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
    public Socket<? extends Collection<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

}
