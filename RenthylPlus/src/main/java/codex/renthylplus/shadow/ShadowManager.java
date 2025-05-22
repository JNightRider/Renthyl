/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.CollectorSocket;
import codex.renthyl.sockets.DynamicSocketList;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.Frame;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author codex
 */
public class ShadowManager extends Frame {

    private final AssetManager assetManager;
    private final ResourceAllocator allocator;
    private final DynamicSocketList<TransitiveSocket<Light>, Light> lights = new DynamicSocketList<>(this, () -> new TransitiveSocket<>(this));
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final ShadowQueuePass queues = new ShadowQueuePass();
    private final Collection<ShadowOcclusionPass> occluders = new ArrayList<>();

    public ShadowManager(AssetManager assetManager, ResourceAllocator allocator) {
        this.assetManager = assetManager;
        this.allocator = allocator;
        addSockets(lights, shadowMaps);
    }

    private <T extends Light> void addOcclusion(ShadowOcclusionPass<T> occlusion) {
        occlusion.getOccluders().setUpstream(queues.getOccluders());
        occlusion.getReceivers().setUpstream(queues.getReceivers());
        shadowMaps.addCollectionSource(occlusion.getShadowMaps());
        occluders.add(occlusion);
    }

    public void addDirectionalLightSource(Socket<DirectionalLight> socket, int size, int splits) {
        lights.add(socket);
        DirectionalShadowPass dsp = new DirectionalShadowPass(assetManager, allocator, size, splits);
        dsp.getLight().setUpstream(socket);
        addOcclusion(dsp);
    }

    public void addPointLightSource(Socket<PointLight> socket, int size) {
        lights.add(socket);
        PointShadowPass psp = new PointShadowPass(assetManager, allocator, size);
        psp.getLight().setUpstream(socket);
        addOcclusion(psp);
    }

    public void addSpotLightSource(Socket<SpotLight> socket, int size) {
        lights.add(socket);
        SpotShadowPass ssp = new SpotShadowPass(assetManager, allocator, size);
        ssp.getLight().setUpstream(socket);
        addOcclusion(ssp);
    }

    public void removeLightSource(Socket socket) {
        for (Iterator<ShadowOcclusionPass> it = occluders.iterator(); it.hasNext();) {
            ShadowOcclusionPass p = it.next();
            if (p.getLight().getUpstream() == socket) {
                shadowMaps.removeCollectionSource(p.getShadowMaps());
                it.remove();
            }
        }
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return queues.getGeometry();
    }

    public Socket<GeometryQueue> getOccluders() {
        return queues.getOccluders();
    }

    public Socket<GeometryQueue> getReceivers() {
        return queues.getReceivers();
    }

    public Socket<List<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

}
