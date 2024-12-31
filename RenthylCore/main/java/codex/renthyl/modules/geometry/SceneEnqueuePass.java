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

import codex.boost.export.SavableObject;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.resources.ResourceTicket;
import codex.boost.render.DepthRange;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.util.SpatialWorldParam;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GuiComparator;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.TransparentComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Enqueues geometries into different {@link GeometryQueue}s based on world
 * render bucket value.
 * <p>
 * Outputs vary based on what GeometryQueues are added. If default queues are
 * added (via {@link #SceneEnqueuePass(boolean, boolean)}), then the outputs
 * include: "Opaque", "Sky", "Transparent", "Gui", "Translucent". All outputs
 * are GeometryQueues.
 * <p>
 * A geometry is placed in queues according to the userdata found at
 * {@link #QUEUE} (expected as String) according to ancestor inheritance, or the
 * value returned by {@link Geometry#getQueueBucket()} (converted to String).
 * Userdata value (if found) trumps queue bucket value.
 * <p>
 * If a geometry does not have a queue name corresponding to a queue built
 * by this pass, the geometry will be added to the {@link #SINGLE_QUEUE}, if
 * it exists. Otherwise the geometry is discarded.
 * 
 * @author codex
 */
public class SceneEnqueuePass extends RenderPass {
    
    public static final String SINGLE_QUEUE = "Queue";
    public static final String
            OPAQUE = "Opaque",
            SKY = "Sky",
            TRANSPARENT = "Transparent",
            GUI = "Gui",
            TRANSLUCENT = "Translucent";
    
    private final HashMap<String, Queue> queues = new HashMap<>();
    private String defaultQueue;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        for (Queue b : queues.values()) {
            b.geometry = addOutput(b.name);
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (Queue b : queues.values()) {
            declare(null, b.geometry);
        }
    }
    @Override
    protected void execute(FGRenderContext context) {
        ViewPort vp = context.getViewPort();
        List<Spatial> scenes = vp.getScenes();
        for (int i = scenes.size()-1; i >= 0; i--) {
            vp.getCamera().setPlaneState(0);
            queueSubScene(context, scenes.get(i));
        }
        for (Queue b : queues.values()) {
            resources.setPrimitive(b.geometry, b.queue);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {
        for (Queue b : queues.values()) {
            b.queue.clear();
        }
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        ArrayList<Queue> list = new ArrayList<>();
        list.addAll(queues.values());
        out.writeSavableArrayList(list, "buckets", new ArrayList<>());
        out.write(defaultQueue, "defaultQueue", OPAQUE);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        ArrayList<Savable> list = in.readSavableArrayList("buckets", new ArrayList<>());
        for (Savable s : list) {
            Queue b = (Queue)s;
            queues.put(b.name, b);
        }
        defaultQueue = in.readString("defaultQueue", OPAQUE);
    }
    
    private void queueSubScene(FGRenderContext context, Spatial spatial) {
        // get target bucket
        SpatialWorldParam.RenderQueueParam.apply(spatial);
        String value = SpatialWorldParam.RenderQueueParam.getWorldValue(spatial);
        if (value == null) {
            throw new NullPointerException("World render queue value was not calculated correctly.");
        }
        Queue queue = queues.get(value);
        if (queue == null) {
            queue = queues.get(defaultQueue);
        }
        if (spatial instanceof Node) {
            for (Spatial s : ((Node)spatial).getChildren()) {
                queueSubScene(context, s);
            }
        } else if (spatial instanceof Geometry) {
            Geometry g = (Geometry)spatial;
            if (g.getMaterial() == null) {
                throw new IllegalStateException("No material is set for geometry: " + g.getName());
            }
            queue.queue.add(g);
        }
    }
    
    public final SceneEnqueuePass add(String name, GeometryQueue queue) {
        if (isAssigned()) {
            throw new IllegalStateException("Cannot add queues while assigned to a framegraph.");
        }
        if (defaultQueue == null) {
            defaultQueue = name;
        }
        queues.put(name, new Queue(name, queue));
        return this;
    }
    public void setDefaultQueue(String defaultQueue) {
        this.defaultQueue = defaultQueue;
    }
    public String getDefaultQueue() {
        return defaultQueue;
    }
    
    public static SceneEnqueuePass withLegacyQueues() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.add(OPAQUE, new GeometryQueue(new OpaqueComparator()));
        p.add(TRANSPARENT, new GeometryQueue(new TransparentComparator()));
        p.add(TRANSLUCENT, new GeometryQueue(new TransparentComparator()));
        GeometryQueue sky = new GeometryQueue();
        sky.addMode(RenderMode.depthRange(DepthRange.REAR));
        p.add(SKY, sky);
        GeometryQueue gui = new GeometryQueue(new GuiComparator());
        gui.addMode(RenderMode.depthRange(DepthRange.FRONT));
        gui.addMode(RenderMode.parallelProjection(true));
        p.add(GUI, gui);
        return p;
    }
    public static SceneEnqueuePass withSingleQueue() {
        SceneEnqueuePass p = new SceneEnqueuePass();
        p.add(SINGLE_QUEUE, new GeometryQueue(new OpaqueComparator()));
        return p;
    }
    
    private static class Queue implements Savable {
        
        public String name;
        public GeometryQueue queue;
        public ResourceTicket<GeometryQueue> geometry;
        
        public Queue() {}
        public Queue(String name, GeometryQueue queue) {
            this.name = name;
            this.queue = queue;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(name, "name", "Opaque");
            out.write(queue, "queue", new GeometryQueue());
        }
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            name = in.readString("name", "Opaque");
            queue = SavableObject.readSavable(in, "queue", GeometryQueue.class, new GeometryQueue());
        }
        
    }
    
}
