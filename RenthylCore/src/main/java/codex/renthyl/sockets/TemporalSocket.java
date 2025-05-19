package codex.renthyl.sockets;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.render.Renderable;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.resources.ResourceWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TemporalSocket <T> extends TerminalSocket<List<T>> {

    private final ResourceAllocator allocator;
    private final ResourceDef<T> def;
    private final SnapshotSocket<T>[] snapshots;
    private final ArrayList<T> resources = new ArrayList<>();
    private int startingPosition = Integer.MAX_VALUE;

    public TemporalSocket(Renderable task, ResourceAllocator allocator, ResourceDef<T> def, int memory) {
        super(task);
        this.allocator = allocator;
        this.def = def;
        this.snapshots = new SnapshotSocket[memory];
        this.snapshots[0] = new CurrentSocket(this.task);
        for (int i = 1; i < this.snapshots.length; i++) {
            this.snapshots[i] = new SnapshotSocket(this.task);
        }
    }

    @Override
    public void update(float tpf) {
        for (int i = snapshots.length - 1; i >= 0; i--) {
            SnapshotSocket<T> s = snapshots[i];
            if (i > 0) {
                s.switchWrappers(snapshots[i - 1]);
            }
            if (!s.evaluate(def)) {
                s.releaseWrapper();
            }
            s.update(tpf);
        }
    }

    @Override
    public void reference(int position) {
        super.reference(position);
        for (Socket<T> s : snapshots) {
            s.reference(position);
        }
        startingPosition = Math.min(startingPosition, position);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return super.isUpstreamAvailable(queuePosition) && Arrays.stream(snapshots).allMatch(s -> s.isUpstreamAvailable(queuePosition));
    }

    @Override
    public List<T> acquire() {
        for (int i = 0; i < snapshots.length; i++) {
            if (i < resources.size()) {
                resources.set(i, snapshots[i].acquire());
            } else {
                resources.add(snapshots[i].acquire());
            }
        }
        return resources;
    }

    @Override
    public void resetSocket() {
        startingPosition = Integer.MAX_VALUE;
        for (Socket<T> s : snapshots) {
            s.resetSocket();
        }
    }

    public Socket<T> getSnapshot(int i) {
        return snapshots[i];
    }

    public Socket<T> getCurrent() {
        return snapshots[0];
    }

    public ResourceDef<T> getDef() {
        return def;
    }

    public int getNumSnapshots() {
        return snapshots.length;
    }

    private static class SnapshotSocket <T> extends TerminalSocket<T> {

        protected ResourceWrapper wrapper;
        protected T value;

        public SnapshotSocket(Renderable task) {
            super(task);
        }

        @Override
        public void update(float tpf) {}

        @Override
        public T acquire() {
            return value;
        }

        @Override
        public void resetSocket() {}

        public void switchWrappers(SnapshotSocket<T> socket) {
            ResourceWrapper w = socket.wrapper;
            T v = socket.value;
            socket.wrapper = this.wrapper;
            socket.value = this.value;
            this.wrapper = w;
            this.value = v;
        }

        public boolean evaluate(ResourceDef<T> def) {
            return value == null || def.evaluateResource(value) != null;
        }

        public void releaseWrapper() {
            if (wrapper != null) {
                wrapper.release();
                wrapper = null;
                value = null;
            }
        }

    }

    private class CurrentSocket extends SnapshotSocket<T> {

        public CurrentSocket(Renderable task) {
            super(task);
        }

        @Override
        public T acquire() {
            if (value == null) {
                wrapper = allocator.allocate(def, startingPosition, Integer.MAX_VALUE);
                value = def.conformResource(wrapper.get());
            }
            return super.acquire();
        }

    }

}
