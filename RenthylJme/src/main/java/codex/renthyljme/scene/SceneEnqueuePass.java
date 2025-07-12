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
package codex.renthyljme.scene;

import codex.renthyljme.FrameGraphContext;
import codex.renthyljme.geometry.BasicGeometryQueue;
import codex.renthyljme.geometry.GeometryQueue;
import codex.boost.render.DepthRange;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.collections.SocketMap;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyljme.RasterTask;
import com.jme3.renderer.queue.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.*;

/**
 * Enqueues geometries into different {@link GeometryQueue GeometryQueues} based on world
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
public class SceneEnqueuePass extends RasterTask {

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
    protected void renderTask() {
        List<Spatial> scenes = context.getViewPort().getScenes();
        for (int i = scenes.size() - 1; i >= 0; i--) {
            queueSubScene(scenes.get(i), defaultQueue);
        }
    }

    @Override
    public void reset() {
        super.reset();
        for (ValueSocket<GeometryQueue> q : queueMap.values()) {
            q.getValue().clear();
        }
    }

    private void queueSubScene(Spatial spatial, String parentParam) {
        String queueParam = spatial.getUserData(QUEUE_PARAM);
        if (queueParam == null) {
            queueParam = spatial.getQueueBucket().name();
        }
        if (queueParam.equals(INHERIT_QUEUE)) {
            queueParam = parentParam;
        }
        if (spatial instanceof Node) {
            for (Spatial s : ((Node)spatial).getChildren()) {
                queueSubScene(s, queueParam);
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

    /**
     * Adds a queue that can be filled with geometries from the viewport's scenes.
     *
     * @param name
     * @param queue
     */
    public void addQueue(String name, GeometryQueue queue) {
        if (defaultQueue == null) {
            defaultQueue = name;
        }
        queueMap.put(name, new ValueSocket<>(this, Objects.requireNonNull(queue, "GeometryQueue cannot be null.")));
    }

    /**
     * Names the queue geometries are put into be default.
     *
     * @param defaultQueue
     */
    public void setDefaultQueue(String defaultQueue) {
        this.defaultQueue = defaultQueue;
    }

    /**
     * Gets the name of the default queue.
     *
     * @return
     */
    public String getDefaultQueue() {
        return defaultQueue;
    }

    /**
     * Gets socket for all queues by name (output).
     *
     * @return
     */
    public Socket<Map<String, GeometryQueue>> getQueues() {
        return queueMap;
    }

    /**
     * Creates a SceneEnqueuePass with queues corresponding to JMonkeyEngine's own RenderQueues.
     *
     * @return
     */
    public static SceneEnqueuePass withLegacyQueues() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.addQueue(OPAQUE, new BasicGeometryQueue(new OpaqueComparator()));
        p.addQueue(TRANSPARENT, new BasicGeometryQueue(new TransparentComparator()));
        p.addQueue(TRANSLUCENT, new BasicGeometryQueue(new TransparentComparator()));
        p.addQueue(SKY, new BasicGeometryQueue(new NullComparator()) {
            @Override
            public void applySettings(FrameGraphContext context) {
                context.getDepthRange().pushValue(DepthRange.REAR);
            }
            @Override
            public void restoreSettings(FrameGraphContext context) {
                context.getDepthRange().pop();
            }
        });
        p.addQueue(GUI, new BasicGeometryQueue(new GuiComparator()) {
            @Override
            public void applySettings(FrameGraphContext context) {
                context.getDepthRange().pushValue(DepthRange.FRONT);
                context.getCamera().pushValue(true);
            }
            @Override
            public void restoreSettings(FrameGraphContext context) {
                context.getDepthRange().pop();
                context.getCamera().pop();
            }
        });
        return p;
    }

    /**
     * Creates a SceneEnqueuePass with only one queue to fill.
     *
     * @return
     */
    public static SceneEnqueuePass withSingleQueue() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.addQueue(SINGLE_QUEUE, new BasicGeometryQueue(new OpaqueComparator()));
        return p;
    }
    
}
