package codex.renthyl.sockets.collections;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;

import java.util.*;
import java.util.function.Predicate;

/**
 * Socket which collects elements from any number of unique upstream sources into a single {@link List}.
 *
 * <p>CollectorSocket supports 4 types of upstream sockets: Collection&lt;T&gt;, T[], Map&lt;?, T&gt;, and T.
 * Upstream sockets are processed in that order as well (first collections, then arrays, etc). The order
 * in which elements are transfered to the resulting List is determined by the order in which the sockets
 * are added; earlier socket's elements are added first. The order of elements from a single Collection, Map,
 * or array source is determined by their respective iterators.</p>
 *
 * @param <T>
 * @author codex
 */
public class CollectorSocket <T> implements Socket<List<T>> {

    private final Renderable task;
    private final List<Socket<? extends Collection<T>>> collectionSources = new ArrayList<>();
    private final List<Socket<? extends T[]>> arraySources = new ArrayList<>();
    private final List<Socket<? extends Map<?, T>>> mapSources = new ArrayList<>();
    private final List<Socket<T>> sources = new ArrayList<>();
    private final List<T> target;
    private int activeRefs = 0;
    private boolean staged = false;

    /**
     * Creates a CollectorSocket that collects to an {@link ArrayList}.
     *
     * @param task underlying task
     */
    public CollectorSocket(Renderable task) {
        this(task, new ArrayList<>());
    }

    /**
     *
     * @param task underlying task
     * @param target list to collect to
     */
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
                && arraySources.stream().allMatch(p)
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
        for (Socket<? extends T[]> s : arraySources) {
            T[] v = s.acquire();
            if (v != null) {
                target.addAll(Arrays.asList(v));
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
        staged = false;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (!staged) {
            staged = true;
            task.preStage(globals);
            for (Socket<? extends Collection<T>> s : collectionSources) {
                s.stage(globals, queue);
            }
            for (Socket<? extends T[]> s : arraySources) {
                s.stage(globals, queue);
            }
            for (Socket<? extends Map<?, T>> s : mapSources) {
                s.stage(globals, queue);
            }
            for (Socket<T> s : sources) {
                s.stage(globals, queue);
            }
            task.stage(globals, queue);
        }
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        for (Socket<? extends Collection<T>> s : collectionSources) {
            s.reference(queuePosition);
        }
        for (Socket<? extends T[]> s : arraySources) {
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
        for (Socket<? extends T[]> s : arraySources) {
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

    /**
     * Adds a upstream collection source.
     *
     * @param source
     */
    public void addCollectionSource(Socket<? extends Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(source);
    }

    /**
     * Adds an upstream collection source at the specified index.
     *
     * @param index
     * @param source
     */
    public void addCollectionSource(int index, Socket<? extends Collection<T>> source) {
        assertNoActiveReferences();
        collectionSources.add(index, source);
    }

    /**
     * Adds an upstream array source.
     *
     * @param source
     */
    public void addArraySource(Socket<? extends T[]> source) {
        assertNoActiveReferences();
        arraySources.add(source);
    }

    /**
     * Adds an upstream array source at the specified index.
     *
     * @param index
     * @param source
     */
    public void addArraySource(int index, Socket<? extends T[]> source) {
        assertNoActiveReferences();
        arraySources.add(index, source);
    }

    /**
     * Adds an upstream map source.
     *
     * @param source
     */
    public void addMapSource(Socket<? extends Map<?, T>> source) {
        assertNoActiveReferences();
        mapSources.add(source);
    }

    /**
     * Adds an upstream map source at the specified index.
     *
     * @param index
     * @param source
     */
    public void addMapSource(int index, Socket<? extends Map<?, T>> source) {
        assertNoActiveReferences();
        mapSources.add(index, source);
    }

    /**
     * Adds a regular upstream source.
     *
     * @param source
     */
    public void addSource(Socket<T> source) {
        assertNoActiveReferences();
        sources.add(source);
    }

    /**
     * Adds a regular upstream source at the specified index.
     *
     * @param index
     * @param source
     */
    public void addSource(int index, Socket<T> source) {
        assertNoActiveReferences();
        sources.add(index, source);
    }

    /**
     * Removes the collection source.
     *
     * @param source
     */
    public void removeCollectionSource(Socket source) {
        assertNoActiveReferences();
        collectionSources.remove(source);
    }

    /**
     * Removes the array source.
     *
     * @param source
     */
    public void removeArraySource(Socket source) {
        assertNoActiveReferences();
        arraySources.remove(source);
    }

    /**
     * Removes the map source.
     *
     * @param source
     */
    public void removeMapSource(Socket source) {
        assertNoActiveReferences();
        mapSources.remove(source);
    }

    /**
     * Removes the regular source.
     *
     * @param source
     */
    public void removeSource(Socket source) {
        assertNoActiveReferences();
        sources.remove(source);
    }

    /**
     * Removes the source socket from all source lists (collections, arrays, maps, and regular).
     *
     * @param socket
     */
    public void remove(Socket socket) {
        assertNoActiveReferences();
        collectionSources.remove(socket);
        arraySources.remove(socket);
        mapSources.remove(socket);
        sources.remove(socket);
    }

    /**
     * Clears all upstream sources.
     */
    public void clear() {
        assertNoActiveReferences();
        collectionSources.clear();
        arraySources.clear();
        mapSources.clear();
        sources.clear();
    }

    /**
     * Returns the number of upstream collection sources.
     *
     * @return
     */
    public int getNumCollectionSources() {
        return collectionSources.size();
    }

    /**
     * Returns the number of upstream array sources.
     *
     * @return
     */
    public int getNumArraySources() {
        return arraySources.size();
    }

    /**
     * Returns the number of upstream map sources.
     *
     * @return
     */
    public int getNumMapSources() {
        return mapSources.size();
    }

    /**
     * Returns the number of regular upstream sources (not including collection, array, or map sources).
     *
     * @return
     */
    public int getNumSources() {
        return sources.size();
    }

}
