/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.renthyl.GlobalAttributes;
import codex.renthyljme.geometry.BasicGeometryQueue;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyl.tasks.AbstractTask;
import codex.renthyljme.utils.SpatialWorldParam;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class ShadowQueuePass extends AbstractTask {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final ValueSocket<GeometryQueue> occluders = new ValueSocket<>(this, new BasicGeometryQueue(new OpaqueComparator()));
    private final ValueSocket<GeometryQueue> receivers = new ValueSocket<>(this, new BasicGeometryQueue(new OpaqueComparator()));

    public ShadowQueuePass() {
        addSockets(geometry, occluders, receivers);
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        super.stage(globals, queue);
    }

    @Override
    protected void renderTask() {
        Collection<GeometryQueue> queues = geometry.acquire();
        for (GeometryQueue q : queues) {
            for (Geometry g : q) {
                ShadowMode mode = SpatialWorldParam.getWorldParameter(g, ShadowMode.Inherit, ShadowMode.Off, Spatial::getLocalShadowMode);
                if (mode != null && mode != ShadowMode.Off) {
                    boolean all = mode == ShadowMode.CastAndReceive;
                    if (all || mode == ShadowMode.Cast) {
                        occluders.getValue().add(g);
                    }
                    if (all || mode == ShadowMode.Receive) {
                        receivers.getValue().add(g);
                    }
                }
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        occluders.getValue().clear();
        receivers.getValue().clear();
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public ValueSocket<GeometryQueue> getOccluders() {
        return occluders;
    }

    public ValueSocket<GeometryQueue> getReceivers() {
        return receivers;
    }

}
