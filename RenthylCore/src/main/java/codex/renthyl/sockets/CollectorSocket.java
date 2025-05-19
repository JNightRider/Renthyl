package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class CollectorSocket <T> implements Socket<List<T>> {

    private final Renderable task;
    private final List<Socket<? extends Collection<T>>> collectionSources = new ArrayList<>();
    private final List<Socket<? extends Map<?, T>>> mapSources = new ArrayList<>();
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
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        Predicate<Socket> p = s -> s.isAvailableToDownstream(queuePosition);
        return collectionSources.stream().allMatch(p)
                && mapSources.stream().allMatch(p)
                && sources.stream().allMatch(p);
    }

    @Override
    public List<T> acquire() {
        for (Socket<? extends Collection<T>> s : collectionSources) {
            Collection<T> v = s.acquire();
            if (v != null) {
                target.addAll(v);
            }
        }
        for (Socket<? extends Map<?, T>> s : mapSources) {
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
    public void resetSocket() {
        target.clear();
    }

    @Override
    public void stage(RenderingQueue queue) {
        for (Socket<? extends Collection<T>> s : collectionSources) {
            s.stage(queue);
        }
        for (Socket<? extends Map<?, T>> s : mapSources) {
            s.stage(queue);
        }
        for (Socket<T> s : sources) {
            s.stage(queue);
        }
        task.stage(queue);
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        for (Socket<? extends Collection<T>> s : collectionSources) {
            s.reference(queuePosition);
        }
        for (Socket<? extends Map<?, T>> s : mapSources) {
            s.reference(queuePosition);
        }
        for (Socket<T> s : sources) {
            s.reference(queuePosition);
        }
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        collectionSources.forEach(s -> s.release(queuePosition));
        mapSources.forEach(s -> s.release(queuePosition));
        sources.forEach(s -> s.release(queuePosition));
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public int getResourceUsage() {
        int usage = activeRefs;
        for (Socket<? extends Collection<T>> s : collectionSources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        for (Socket<? extends Map<?, T>> s : mapSources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        for (Socket<T> s : sources) {
            usage = Math.max(usage, s.getResourceUsage());
        }
        return usage;
    }

    public void addCollectionSource(Socket<? extends Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(source);
    }

    public void addCollectionSource(int index, Socket<? extends Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(index, source);
    }

    public void addMapSource(Socket<? extends Map<?, T>> source) {
        assertNoActiveReferences();
        mapSources.add(source);
    }

    public void addMapSource(int index, Socket<? extends Map<?, T>> source) {
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

    public void removeCollectionSource(Socket source) {
        assertNoActiveReferences();
        collectionSources.remove(source);
    }

    public void removeMapSource(Socket source) {
        assertNoActiveReferences();
        mapSources.remove(source);
    }

    public void removeSource(Socket source) {
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
