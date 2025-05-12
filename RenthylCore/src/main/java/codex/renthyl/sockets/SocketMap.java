package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.*;

public class SocketMap <K, T extends Socket<R>, R> extends HashMap<K, T> implements PointerSocket<Map<K, R>> {

    private final Renderable task;
    private Socket<Map<K, R>> upstream;
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
    public void setUpstream(Socket<Map<K, R>> upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    @Override
    public Socket<Map<K, R>> getUpstream() {
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
    public void reset() {
        if (resourceMap != null) {
            resourceMap.clear();
        }
        values().forEach(Socket::reset);
        if (activeRefs != 0) {
            throw new IllegalStateException("Some references were not released.");
        }
    }

    @Override
    public void queue(RenderingQueue queue) {
        if (upstream != null) {
            upstream.queue(queue);
        }
        values().forEach(s -> s.queue(queue));
        task.queue(queue);
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
        if (upstream != null) {
            return upstream.acquire();
        }
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
            R r = orElse == null ? s.acquire() : s.acquire(orElse);
            resourceMap.put(e.getKey(), (orElse == null ? s.acquire() : s.acquire(orElse)));
        }
        return resourceMap;
    }

}
