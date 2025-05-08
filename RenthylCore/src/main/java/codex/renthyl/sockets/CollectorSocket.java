package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CollectorSocket <T> implements Socket<List<T>> {

    private final Renderable task;
    private final List<Socket<Collection<T>>> collectionSources = new ArrayList<>();
    private final List<Socket<Map<Object, T>>> mapSources = new ArrayList<>();
    private final List<Socket<T>> sources = new ArrayList<>();
    private final List<T> target;
    private int activeRefs = 0;

    public CollectorSocket(Renderable task) {
        this(task, new ArrayList<>());
    }
    public CollectorSocket(Renderable task, List<T> target) {
        this.task = task;
        this.target = target;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream() {
        return task.isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return collectionSources.stream().allMatch(Socket::isAvailableToDownstream)
                && mapSources.stream().allMatch(Socket::isAvailableToDownstream)
                && sources.stream().allMatch(Socket::isAvailableToDownstream);
    }

    @Override
    public List<T> acquire() {
        for (Socket<Collection<T>> s : collectionSources) {
            Collection<T> v = s.acquire();
            if (v != null) {
                target.addAll(v);
            }
        }
        for (Socket<Map<Object, T>> s : mapSources) {
            Map<?, T> v = s.acquire();
            if (v != null) {
                target.addAll(v.values());
            }
        }
        for (Socket<T> s : sources) {
            T v = s.acquire();
            if (v != null) {
                target.add(v);
            }
        }
        return target;
    }

    @Override
    public void reset() {
        target.clear();
    }

    @Override
    public void queue(RenderingQueue queue) {
        for (Socket<Collection<T>> s : collectionSources) {
            s.queue(queue);
        }
        for (Socket<Map<Object, T>> s : mapSources) {
            s.queue(queue);
        }
        for (Socket<T> s : sources) {
            s.queue(queue);
        }
        task.queue(queue);
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        for (Socket<Collection<T>> s : collectionSources) {
            s.reference(queuePosition);
        }
        for (Socket<Map<Object, T>> s : mapSources) {
            s.reference(queuePosition);
        }
        for (Socket<T> s : sources) {
            s.reference(queuePosition);
        }
    }

    @Override
    public void release() {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        collectionSources.forEach(Socket::release);
        mapSources.forEach(Socket::release);
        sources.forEach(Socket::release);
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public int getResourceUsage() {
        int usage = activeRefs;
        for (Socket<Collection<T>> s : collectionSources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        for (Socket<Map<Object, T>> s : mapSources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        for (Socket<T> s : sources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        return usage;
    }

    public void addCollectionSource(Socket<Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(source);
    }

    public void addCollectionSource(int index, Socket<Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(index, source);
    }

    public void addMapSource(Socket<Map<Object, T>> source) {
        assertNoActiveReferences();
        mapSources.add(source);
    }

    public void addMapSource(int index, Socket<Map<Object, T>> source) {
        assertNoActiveReferences();
        mapSources.add(index, source);
    }

    public void addSource(Socket<T> source) {
        assertNoActiveReferences();
        sources.add(source);
    }

    public void addSource(int index, Socket<T> source) {
        assertNoActiveReferences();
        sources.add(index, source);
    }

    public void removeCollectionSource(Socket<Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.remove(source);
    }

    public void removeMapSource(Socket<Map<?, T>> source) {
        assertNoActiveReferences();
        mapSources.remove(source);
    }

    public void removeSource(Socket<T> source) {
        assertNoActiveReferences();
        sources.remove(source);
    }

    public void remove(Socket socket) {
        assertNoActiveReferences();
        collectionSources.remove(socket);
        mapSources.remove(socket);
        sources.remove(socket);
    }

    public void clear() {
        assertNoActiveReferences();
        collectionSources.clear();
        mapSources.clear();
        sources.clear();
    }

    public int getNumCollectionSources() {
        return collectionSources.size();
    }

    public int getNumMapSources() {
        return mapSources.size();
    }

    public int getNumSources() {
        return sources.size();
    }

}
