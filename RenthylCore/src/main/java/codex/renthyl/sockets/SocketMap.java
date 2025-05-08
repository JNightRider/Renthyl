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
        this.task = task;
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
    public boolean isAvailableToDownstream() {
        return task.isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return (upstream == null || upstream.isAvailableToDownstream()) && values().stream().allMatch(Socket::isUpstreamAvailable);
    }

    @Override
    public Map<K, R> acquire() {
        return acquireOrElse(null);
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
    public void release() {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release();
        }
        values().forEach(Socket::release);
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
        for (Entry<K, T> e : entrySet()) {
            T s = e.getValue();
            resourceMap.put(e.getKey(), (orElse == null ? s.acquire() : s.acquire(orElse)));
        }
        return resourceMap;
    }

}
