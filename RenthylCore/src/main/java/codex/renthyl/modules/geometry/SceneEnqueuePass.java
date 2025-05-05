/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.modules.geometry;

import codex.renthyl.FGRenderContext;
import codex.renthyl.GeometryQueue;
import codex.boost.render.DepthRange;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.newresources.*;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GuiComparator;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.*;

/**
 * Enqueues geometries into different {@link GeometryQueue}s based on world
 * render bucket value.
 * <p>
 * A geometry is placed in queues according to the userdata found at
 * {@link #QUEUE_PARAM} (expected as String) according to ancestor inheritance, or the
 * value returned by {@link Geometry#getQueueBucket()} (converted to String).
 * Userdata value (if found) trumps queue bucket value.
 * <p>
 * If a geometry does not have a queue name corresponding to a queue built
 * by this pass, the geometry will be added to the {@link #SINGLE_QUEUE}, if
 * it exists. Otherwise the geometry is discarded.
 * 
 * @author codex
 */
public class SceneEnqueuePass extends RenderTask {

    public static final String QUEUE_PARAM = "GeometryRenderQueue";
    public static final String INHERIT_QUEUE = RenderQueue.Bucket.Inherit.name();

    public static final String SINGLE_QUEUE = "Queue";
    public static final String OPAQUE = "Opaque", SKY = "Sky",
            TRANSPARENT = "Transparent", GUI = "Gui", TRANSLUCENT = "Translucent";

    private final SocketMap<String, ValueSocket<GeometryQueue>, GeometryQueue> queueMap = new SocketMap<>(this);
    private String defaultQueue;

    public SceneEnqueuePass() {
        addSockets(queueMap);
    }

    @Override
    protected void renderTask(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        List<Spatial> scenes = vp.getScenes();
        for (int i = scenes.size() - 1; i >= 0; i--) {
            vp.getCamera().setPlaneState(0);
            queueSubScene(context, scenes.get(i), defaultQueue);
        }
        queueMap.values().forEach(Socket::release);
    }

    private void queueSubScene(FGRenderContext context, Spatial spatial, String parentParam) {
        // get target bucket
        String queueParam = spatial.getUserData(QUEUE_PARAM);
        if (queueParam == null) {
            queueParam = spatial.getQueueBucket().name();
        }
        if (queueParam.equals(INHERIT_QUEUE)) {
            queueParam = parentParam;
        }
        if (spatial instanceof Node) {
            for (Spatial s : ((Node)spatial).getChildren()) {
                queueSubScene(context, s, queueParam);
            }
        } else if (spatial instanceof Geometry) {
            Geometry g = (Geometry)spatial;
            if (g.getMaterial() == null) {
                throw new IllegalStateException("No material present for geometry: " + g.getName());
            }
            ValueSocket<GeometryQueue> queueSocket = queueMap.get(queueParam);
            if (queueSocket != null) {
                queueSocket.getValue().add(g);
            }
        }
    }
    
    public void addQueue(String name, GeometryQueue queue) {
        if (defaultQueue == null) {
            defaultQueue = name;
        }
        queueMap.put(name, new ValueSocket<>(this, Objects.requireNonNull(queue, "GeometryQueue cannot be null.")));
    }

    public void setDefaultQueue(String defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

    public String getDefaultQueue() {
        return defaultQueue;
    }

    public SocketMap<String, ?, GeometryQueue> getQueues() {
        return queueMap;
    }
    
    public static SceneEnqueuePass withLegacyQueues() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.addQueue(OPAQUE, new GeometryQueue(new OpaqueComparator()));
        p.addQueue(TRANSPARENT, new GeometryQueue(new TransparentComparator()));
        p.addQueue(TRANSLUCENT, new GeometryQueue(new TransparentComparator()));
        GeometryQueue sky = new GeometryQueue();
        sky.addMode(RenderMode.depthRange(DepthRange.REAR));
        p.addQueue(SKY, sky);
        GeometryQueue gui = new GeometryQueue(new GuiComparator());
        gui.addMode(RenderMode.depthRange(DepthRange.FRONT));
        gui.addMode(RenderMode.parallelProjection(true));
        p.addQueue(GUI, gui);
        return p;
    }

    public static SceneEnqueuePass withSingleQueue() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.addQueue(SINGLE_QUEUE, new GeometryQueue(new OpaqueComparator()));
        return p;
    }
    
}
