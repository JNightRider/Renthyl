/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class SpotShadowPass extends RenderTask implements Occlusion<SpotLight> {

    private final ArgumentSocket<SpotLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new OptionalSocket<>(this, false);
    private final ArgumentSocket<Float> nearFrustum = new ArgumentSocket<>(this, 1f);
    private final SocketList<ShadowMapSocket, ShadowMap> shadowMaps = new SocketList<>(this);
    private final CameraState camera;
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public SpotShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        addSockets(light, occluders, receivers, nearFrustum, shadowMaps);
        shadowMaps.addSocket(new ShadowMapSocket(this, allocator)).setSize(size, size);
        camera = new CameraState(new Camera(size, size), false);
        backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }

    @Override
    protected void renderTask() {

        SpotLight sl = light.acquireOrThrow("Light required");
        camera.getCamera().setLocation(sl.getPosition());
        camera.getCamera().lookAtDirection(sl.getDirection(), camera.getCamera().getUp());
        camera.getCamera().setFrustumPerspective(sl.getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2f, 1f, nearFrustum.acquire(1f), sl.getSpotRange());
        camera.getCamera().update();
        camera.getCamera().updateViewProjection();
        context.getCamera().pushValue(camera);

        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        ShadowMapSocket shadowMap = shadowMaps.getFirst();
        ShadowMap map = shadowMap.acquire();
        map.setLight(sl);
        map.setProjection(camera.getCamera().getViewProjectionMatrix());
        map.setRange(camera.getCamera().getFrustumNear(), camera.getCamera().getFrustumFar());
        shadowMap.setTargetDepth(map.getMap());

        FrameBuffer fbo = shadowMap.getFrameBuffer().acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        occluders.acquireOrThrow("Receiver queue required").render(context);

        context.getCamera().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();
        context.getFrameBuffer().pop();

    }

    @Override
    public ArgumentSocket<SpotLight> getLight() {
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

    public ArgumentSocket<Float> getNearFrustum() {
        return nearFrustum;
    }

}
