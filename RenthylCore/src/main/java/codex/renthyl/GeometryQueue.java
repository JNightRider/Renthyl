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
package codex.renthyl;

import codex.boost.export.SavableObject;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.ListSort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;

/**
 * Queue of ordered geometries for rendering.
 * <p>
 * Similar to {@link GeometryList}, but designed for use in FrameGraphs.
 * Specifically, this can store other GeometryQueues internally, essentially
 * making queues able to merge very quickly and still maintain geometry order.
 *
 * @author codex
 */
public class GeometryQueue implements Iterable<Geometry>, Savable {

    private static final int DEFAULT_SIZE = 32;
    private static final NullComparator NULL_COMPARATOR = new NullComparator();

    private Geometry[] geometries;
    private QueueComparator comparator;
    private final ArrayList<QueueView> views = new ArrayList<>();
    private final ArrayList<GeometryQueue> internalQueues = new ArrayList<>();
    private final ArrayList<RenderMode> modes = new ArrayList<>();
    private final ListSort<Integer> listSort = new ListSort<>();
    private final BoundingBox localBound = new BoundingBox();
    private final boolean gui;
    private int size = 0;
    private boolean allViewsNeedUpdate = true;
    
    public GeometryQueue() {
        this(NULL_COMPARATOR);
    }
    public GeometryQueue(GeometryComparator comparator) {
        this(comparator, false, DEFAULT_SIZE);
    }
    public GeometryQueue(GeometryComparator comparator, boolean gui) {
        this(comparator, gui, DEFAULT_SIZE);
    }
    public GeometryQueue(GeometryComparator comparator, boolean gui, int initialSize) {
        this.comparator = new QueueComparator(comparator);
        this.gui = gui;
        geometries = new Geometry[initialSize];
    }
    
    public void render(FGRenderContext context, GeometryRenderHandler handler) {
        for (RenderMode m : modes) {
            m.apply(context);
        }
        if (size > 0) {
            Camera current = context.getCurrentCamera();
            QueueView view = fetchView(current);
            if (view == null) {
                view = new QueueView(current);
                views.add(view);
            }
            view.render(context, handler);
        }
        for (GeometryQueue q : internalQueues) {
            q.render(context, handler);
        }
        for (RenderMode m : modes) {
            m.reset(context);
        }
    }
    
    public void add(Geometry g) {
        if (size == geometries.length) {
            Geometry[] temp = new Geometry[size * 2];
            System.arraycopy(geometries, 0, temp, 0, size);
            geometries = temp;
        }
        if (size == 0) {
            g.getWorldBound().clone(localBound);
        } else {
            localBound.mergeLocal(g.getWorldBound());
        }
        if (geometries[size] != g) {
            geometries[size] = g;
            if (!allViewsNeedUpdate) for (QueueView v : views) {
                v.setUpdateFlag();
            }
            allViewsNeedUpdate = true;
        }
        size++;
    }
    public void add(GeometryQueue q) {
        internalQueues.add(q);
    }
    public void addMode(RenderMode m) {
        modes.add(m);
    }

    public void clear() {
        size = 0;
        allViewsNeedUpdate = true;
        for (Iterator<QueueView> it = views.iterator(); it.hasNext();) {
            QueueView v = it.next();
            v.setUpdateFlag();
            if (!v.pollIsUsed()) {
                it.remove();
            }
        }
        for (GeometryQueue q : internalQueues) {
            q.clear();
        }
        internalQueues.clear();
    }
    
    public int getNumGeometries() {
        int n = size;
        for (GeometryQueue q : internalQueues) {
            n += q.getNumGeometries();
        }
        return n;
    }
    public BoundingBox getLocalBound() {
        return localBound;
    }
    public BoundingBox getWorldBound(BoundingBox store) {
        if (store == null) {
            store = new BoundingBox(localBound);
        } else {
            localBound.clone(store);
        }
        for (GeometryQueue q : internalQueues) {
            q.mergeWorldBoundInto(store);
        }
        return store;
    }
    public boolean isGui() {
        return gui;
    }
    
    private QueueView fetchView(Camera cam) {
        for (QueueView v : views) {
            if (v.cam == cam) {
                return v;
            }
        }
        return null;
    }
    private void mergeWorldBoundInto(BoundingBox target) {
        target.mergeLocal(localBound);
        for (GeometryQueue q : internalQueues) {
            q.mergeWorldBoundInto(target);
        }
    }
    
    @Override
    public Iterator<Geometry> iterator() {
        return new QueueIterator();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(new SavableObject(comparator.delegate), "comparator", new SavableObject(NULL_COMPARATOR));
        SavableObject.writeFromCollection(out, modes, "modes");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        comparator.setDelegate(SavableObject.read(in, "comparator", new SavableObject(NULL_COMPARATOR), GeometryComparator.class));
        SavableObject.readToCollection(in, "modes", modes);
    }
    
    protected class QueueView {
        
        private final Camera cam;
        private Integer[] queue;
        private int lastSortedSize = -1;
        private boolean updateFlag = true;
        private boolean used = false;
        
        public QueueView(Camera cam) {
            this.cam = Objects.requireNonNull(cam);
        }
        
        public void render(FGRenderContext context, GeometryRenderHandler handler) {
            if (context.getCurrentCamera() != cam) {
                throw new IllegalStateException("Attempted to render queue view with the incorrect camera.");
            }
            sort();
            for (int i = 0; i < size; i++) {
                Geometry g = geometries[queue[i]];
                int planeState = cam.getPlaneState();
                if (CullTag.evaluate(context, g, handler, gui).isInside()) {
                    handler.renderGeometry(context, g);
                }
                cam.setPlaneState(planeState);
                g.queueDistance = Float.NEGATIVE_INFINITY;
            }
            used = true;
        }
        
        public void sort() {
            if (updateFlag) {
                comparator.setCamera(cam);
                if (listSort.getLength() != size) {
                    listSort.allocateStack(size);
                }
                if (queue == null || queue.length < size) {
                    queue = new Integer[geometries.length];
                    lastSortedSize = -1;
                }
                // Repopulate the index queue if there is a chance that
                // necessary indices would be missing from the lower part.
                if (size < lastSortedSize || lastSortedSize < 0) {
                    for (int i = 0; i < queue.length; i++) {
                        queue[i] = i;
                    }
                }
                lastSortedSize = size;
                listSort.sort(queue, comparator);
                updateFlag = allViewsNeedUpdate = false;
            }
        }
        
        public void setUpdateFlag() {
            updateFlag = true;
        }
        
        public boolean pollIsUsed() {
            boolean u = used;
            used = false;
            return u;
        }
        
    }
    protected class QueueComparator implements Comparator<Integer> {
        
        private GeometryComparator delegate;
        
        public QueueComparator(GeometryComparator delegate) {
            this.delegate = delegate;
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            return delegate.compare(geometries[o1], geometries[o2]);
        }
        
        public void setDelegate(GeometryComparator delegate) {
            this.delegate = delegate;
        }
        
        public void setCamera(Camera cam) {
            delegate.setCamera(cam);
        }
        
    }
    protected class QueueIterator implements Iterator<Geometry> {
        
        private int index = 0;
        private int internalIndex = 0;
        private Iterator<Geometry> internal;
        
        @Override
        public boolean hasNext() {
            if (index < size) {
                return true;
            }
            while (true) {
                if (internal != null && internal.hasNext()) {
                    return true;
                }
                if (internalIndex >= internalQueues.size()) {
                    return false;
                }
                internal = internalQueues.get(internalIndex++).iterator();
            }
        }

        @Override
        public Geometry next() {
            if (index < size) {
                return geometries[index++];
            } else {
                return internal.next();
            }
        }
        
    }
    protected static class CullTag implements Savable {
        
        public static final String TAG_USERDATA = "codex.renthyl.GeometryQueue.CullTag:userdata";
        private static final Object evalLock = new Object();
        private static long cullVersion = 1; // start at 1; 0 indicates unknown; -1 indicates invalid
        
        private final Spatial spatial;
        private long version = 0;
        private Visibility state = Visibility.InsidePartial;
        
        public CullTag() {
            this.spatial = null;
            this.version = -1;
        }
        private CullTag(Spatial spatial) {
            this.spatial = spatial;
            attachToSpatial();
        }
        
        private void attachToSpatial() {
            spatial.setUserData(TAG_USERDATA, this);
        }
        
        public Visibility solve(FGRenderContext context, GeometryRenderHandler handler, boolean gui, long version) {
            if (this.version != version) {
                this.version = version;
                Visibility result = evaluate(context, spatial.getParent(), handler, gui, version);
                state = handler.evaluateSpatialVisibility(context, spatial, result, gui);
            }
            return state;
        }
        
        public boolean isValid() {
            return version >= 0;
        }
        
        private static Visibility evaluate(FGRenderContext context, Spatial spatial, GeometryRenderHandler handler, boolean gui, long version) {
            if (spatial == null) {
                return Visibility.OutsidePartial;
            }
            CullTag tag = spatial.getUserData(TAG_USERDATA);
            if (tag == null || !tag.isValid()) {
                tag = new CullTag(spatial);
            }
            return tag.solve(context, handler, gui, version);
        }
        
        public static Visibility evaluate(FGRenderContext context, Spatial spatial, GeometryRenderHandler handler, boolean gui) {
            synchronized (evalLock) {
                return evaluate(context, spatial, handler, gui, cullVersion++);
            }
        }
        
        @Override
        public void write(JmeExporter ex) throws IOException {}
        @Override
        public void read(JmeImporter im) throws IOException {}
        
    }

}
