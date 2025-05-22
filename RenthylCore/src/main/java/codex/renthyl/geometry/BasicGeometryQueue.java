package codex.renthyl.geometry;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.render.CameraState;
import com.jme3.bounding.BoundingVolume;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.ListSort;

import java.io.IOException;
import java.util.*;

public class BasicGeometryQueue implements GeometryQueue {

    private static final GeometryComparator NULL_COMPARATOR = new NullComparator();

    public static final Sorter<Integer> TIM_SORT = (a, i, c) -> Arrays.sort(a, 0, i, c);

    public static final Sorter<Integer> JME_SORT = new Sorter<>() {
        private final ListSort<Integer> sort = new ListSort<>();
        @Override
        public void sort(Integer[] array, int size, Comparator<Integer> comparator) {
            if (sort.getLength() != size) {
                sort.allocateStack(size);
            }
            sort.sort(array, comparator);
        }
    };

    private final ArrayList<Geometry> geometries = new ArrayList<>();
    private final Collection<QueueView> views = new ArrayList<>();
    private final Sorter<Integer> sorter;
    private final IndexComparator comparator;

    public BasicGeometryQueue() {
        this(JME_SORT, NULL_COMPARATOR);
    }
    public BasicGeometryQueue(Sorter<Integer> sorter) {
        this(sorter, NULL_COMPARATOR);
    }
    public BasicGeometryQueue(GeometryComparator comparator) {
        this(JME_SORT, comparator);
    }
    public BasicGeometryQueue(Sorter<Integer> sorter, GeometryComparator comparator) {
        this.sorter = sorter;
        this.comparator = new IndexComparator(comparator);
    }

    @Override
    public int render(FrameGraphContext context, GeometryRenderHandler handler) {
        CameraState camera = context.getCamera().getValue();
        comparator.setCamera(camera.getCamera());
        return getView(camera).render(context, handler);
    }

    @Override
    public void applySettings(FrameGraphContext context) {}

    @Override
    public void restoreSettings(FrameGraphContext context) {}

    @Override
    public Iterator<Geometry> iterator() {
        return geometries.iterator();
    }

    @Override
    public void add(Geometry g) {
        for (QueueView v : views) {
            v.addIndex(geometries.size());
        }
        geometries.add(g);
    }

    @Override
    public int size() {
        return geometries.size();
    }

    @Override
    public void clear() {
        geometries.clear();
        views.removeIf(v -> !v.cycle());
    }

    private QueueView getView(CameraState camera) {
        for (QueueView v : views) {
            if (camera.identical(v.getLastRenderedCamera())) {
                return v;
            }
        }
        for (QueueView v : views) {
            if (v.isAbandoned()) {
                return v;
            }
        }
        QueueView v = new QueueView(2);
        views.add(v);
        return v;
    }

    private class QueueView {

        private CameraState lastRenderedCamera;
        private Integer[] queue;
        private boolean updateFlag = true;
        private int size = 0;
        private final int duration;
        private int timeout;

        public QueueView(int duration) {
            this.timeout = this.duration = duration;
        }

        public void addIndex(int index) {
            updateFlag = true;
            if (queue != null && index >= size) {
                if (size == queue.length) {
                    Integer[] temp = new Integer[size * 2];
                    System.arraycopy(queue, 0, temp, 0, queue.length);
                    queue = temp;
                }
                queue[size++] = index;
            }
        }

        public int render(FrameGraphContext context, GeometryRenderHandler handler) {
            if (queue == null) {
                initializeQueue();
            }
            if (size > 1 && updateFlag) {
                sorter.sort(queue, size, comparator);
                updateFlag = false;
            }
            lastRenderedCamera = context.getCamera().getValue();
            comparator.setCamera(lastRenderedCamera.getCamera());
            CullTag.begin(lastRenderedCamera);
            int rendered = 0;
            for (int i = 0; i < size; i++) {
                Integer j = queue[i];
                if (j == null || j >= geometries.size()) {
                    size = i;
                    break;
                }
                Geometry g = geometries.get(j);
                if (CullTag.evaluate(g, handler).isInside()) {
                    handler.renderGeometry(context, g);
                    rendered++;
                }
                g.queueDistance = Float.NEGATIVE_INFINITY;
            }
            timeout = duration;
            return rendered;
        }

        public boolean cycle() {
            return timeout-- >= 0;
        }

        public boolean isAbandoned() {
            return timeout == 0;
        }

        public CameraState getLastRenderedCamera() {
            return lastRenderedCamera;
        }

        private void initializeQueue() {
            size = geometries.size();
            queue = new Integer[size * 2];
            for (int i = 0; i < size; i++) {
                queue[i] = i;
            }
        }

    }

    private class IndexComparator implements Comparator<Integer> {

        private GeometryComparator comparator;

        public IndexComparator(GeometryComparator comparator) {
            setComparator(comparator);
        }

        public void setComparator(GeometryComparator comparator) {
            this.comparator = Objects.requireNonNull(comparator, "Comparator cannot be null.");
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            // push invalid indices to the end
            int s = geometries.size();
            if (o1 >= s && o2 >= s) {
                return 0;
            } else if (o1 >= s) {
                return 1;
            } else if (o2 >= s) {
                return -1;
            }
            return comparator.compare(geometries.get(o1), geometries.get(o2));
        }

        public void setCamera(Camera camera) {
            comparator.setCamera(camera);
        }

    }

    public static class CullTag implements Savable {

        public static final String TAG_USERDATA = CullTag.class.getName() + ":userdata";
        private static CameraState lastEvaluatedCamera;
        private static long cullVersion = 0L;

        private final Spatial spatial;
        private long version = 0L;
        private Visibility state;

        public CullTag() {
            this(null);
        }

        private CullTag(Spatial spatial) {
            this.spatial = spatial;
            this.spatial.setUserData(TAG_USERDATA, this);
        }

        private Visibility evaluate(CameraState camera, GeometryRenderHandler handler) {
            if (state != null && version >= cullVersion) {
                return state;
            }
            Visibility parent = evaluate(spatial.getParent(), handler);
            Spatial.CullHint hint = spatial.getLocalCullHint();
            switch (hint) {
                case Never: state = Visibility.get(true, parent != Visibility.Inside); break;
                case Always: state = Visibility.get(false, parent != Visibility.Outside); break;
                case Dynamic: state = (parent.isPartial() ? handler.evaluateSpatialCulling(camera, spatial) : parent); break;
                case Inherit: {
                    if (parent != Visibility.InsidePartial) {
                        // if the parent visibility is outside partial, the parent's
                        // cull hint is guaranteed to always cull
                        state = parent;
                    } else {
                        // there are only two ways the parent visibility can be inside and partial:
                        //  1) the parent is dynamic and partially inside the frustum
                        //  2) the parent is never culled
                        Spatial.CullHint worldHint = spatial.getCullHint();
                        switch (worldHint) {
                            case Never: state = parent; break;
                            case Always: state = Visibility.OutsidePartial; break; // this should technically never occur
                            case Dynamic: state = handler.evaluateSpatialCulling(camera, spatial); break;
                            default: throw new IllegalStateException("Inherited spatial cull hint cannot be " + worldHint + " in this context.");
                        }
                    }
                } break;
            }
            version = cullVersion;
            return state;
        }

        public Visibility getState() {
            return state;
        }

        public boolean isValid() {
            return spatial != null;
        }

        public static Visibility evaluate(Spatial spatial, GeometryRenderHandler handler) {
            if (spatial == null) {
                return Visibility.InsidePartial;
            }
            CullTag tag = spatial.getUserData(TAG_USERDATA);
            if (tag == null || !tag.isValid()) {
                tag = new CullTag(spatial);
            }
            return tag.evaluate(lastEvaluatedCamera, handler);
        }

        public static void begin(CameraState camera) {
            // don't recalculate culling if the correct calculations are at least partially underway
            if (!camera.identical(lastEvaluatedCamera)) {
                cullVersion++;
                lastEvaluatedCamera = camera;
            }
        }

        @Override
        public void write(JmeExporter ex) {}

        @Override
        public void read(JmeImporter im) {}

    }

}
