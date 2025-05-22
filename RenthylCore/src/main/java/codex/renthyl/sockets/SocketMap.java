package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

import java.util.*;
import java.util.function.IntFunction;

public class SocketMap <K, T extends Socket<? extends R>, R> extends HashMap<K, T> implements PointerSocket<Map<K, R>> {

    protected final Renderable task;
    private Socket<? extends Map<K, R>> upstream;
    private Map<K, R> resourceMap;
    private int activeRefs = 0;

    public SocketMap(Renderable task) {
        this(task, null);
    }
    public SocketMap(Renderable task, Map<K, R> resourceMap) {
        this.task = task;
        this.resourceMap = resourceMap;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public void setUpstream(Socket<? extends Map<K, R>> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    @Override
    public Socket<? extends Map<K, R>> getUpstream() {
        return upstream;
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return (upstream == null || upstream.isAvailableToDownstream(queuePosition)) && values().stream().allMatch(t -> t.isUpstreamAvailable(queuePosition));
    }

    @Override
    public Map<K, R> acquire() {
        if (resourceMap == null) {
            resourceMap = new HashMap<>();
        } else {
            resourceMap.clear();
        }
        Map<K, R> defaults = upstream != null ? upstream.acquire() : null;
        for (Entry<K, T> e : entrySet()) {
            R r = e.getValue().acquire();
            if (r == null && defaults != null) {
                r = defaults.get(e.getKey());
            }
            resourceMap.put(e.getKey(), r);
        }
        return resourceMap;
    }

    @Override
    public Map<K, R> acquire(Map<K, R> target) {
        if (resourceMap == null) {
            resourceMap = target;
        }
        return acquireOrElse(null);
    }

    @Override
    public void resetSocket() {
        if (resourceMap != null) {
            resourceMap.clear();
        }
        values().forEach(Socket::resetSocket);
        if (activeRefs != 0) {
            throw new IllegalStateException("Some references were not released.");
        }
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (upstream != null) {
            upstream.stage(globals, queue);
        }
        values().forEach(s -> s.stage(globals, queue));
        task.stage(globals, queue);
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
        values().forEach(s -> s.reference(queuePosition));
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
        values().forEach(t -> t.release(queuePosition));
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public int getResourceUsage() {
        int usage = activeRefs;
        if (upstream != null) {
            usage = Math.max(usage, upstream.getResourceUsage());
        }
        for (T s : values()) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        return usage;
    }

    public Map<K, R> acquireOrElse(R orElse) {
        if (resourceMap == null) {
            resourceMap = new HashMap<>();
        } else {
            resourceMap.clear();
        }
        if (upstream != null) {
            resourceMap.putAll(upstream.acquire());
        }
        Map<K, R> defaults = upstream != null ? upstream.acquire() : null;
        for (Entry<K, T> e : entrySet()) {
            T s = e.getValue();
            R r = s.acquire();
            if (r == null) {
                r = defaults == null ? orElse : defaults.getOrDefault(e.getKey(), orElse);
            }
            resourceMap.put(e.getKey(), r);
        }
        return resourceMap;
    }

    @SafeVarargs
    public final R[] acquireArray(IntFunction<R[]> factory, K... order) {
        R[] array = factory.apply(order.length);
        Map<K, R> map = acquire();
        for (int i = 0; i < order.length; i++) {
            R r = map.get(order[i]);
            if (r == null) {
                throw new NullPointerException("Expected \"" + order[i] + "\" to exist.");
            }
            array[i] = r;
        }
        return array;
    }

}
