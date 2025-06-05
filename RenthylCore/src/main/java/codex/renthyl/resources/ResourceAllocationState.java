package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.renderer.RenderManager;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Basic resource allocator.
 *
 * <p>All resources is disposed on cleanup.</p>
 *
 * @author codex
 */
public class ResourceAllocationState extends BaseAppState implements ResourceAllocator<ResourceWrapper> {

    private final Map<Long, AllocatedResource> resources = new ConcurrentHashMap<>();
    private long nextId = 0;
    private int retryThreshold = 4;
    private int timeoutLength = 1;

    @Override
    protected void initialize(Application app) {}
    @Override
    protected void cleanup(Application app) {}
    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
    @Override
    public void render(RenderManager rm) {
        flush();
    }

    @Override
    public ResourceWrapper allocate(ResourceDef def, int start, int end) {
        if (!isEnabled()) {
            throw new IllegalStateException("Cannot safely allocate resources when disabled.");
        }
        AllocatedResource target = null;
        float lowestEval = Float.MAX_VALUE;
        int selections = 0;
        for (AllocatedResource res : resources.values()) {
            if (res.isAvailable(start, end)) {
                Float eval = def.evaluateResource(res.get());
                if (eval == null) {
                    continue;
                }
                selections++;
                if (def.isPerfectEvaluation(eval)) {
                    if (!res.acquire(start, end)) {
                        continue;
                    }
                    return res;
                }
                if (target == null || eval < lowestEval) {
                    target = res;
                }
            }
        }
        if (target != null && target.acquire(start, end)) {
            return target;
        }
        if (selections >= retryThreshold) {
            return allocate(def, start, end);
        }
        target = new AllocatedResource(nextId++, timeoutLength, def.createResource(), def);
        if (!target.acquire(start, end)) {
            throw new IllegalStateException("Expected new resource to be acquired.");
        }
        resources.put(target.getId(), target);
        return target;
    }

    /**
     * Disposes all held unused resources.
     */
    public void flush() {
        for (Iterator<AllocatedResource> it = resources.values().iterator(); it.hasNext();) {
            AllocatedResource res = it.next();
            if (res.isAvailable() && !res.cycle()) {
                res.dispose();
                it.remove();
            }
        }
    }

    /**
     * Disposes all held resources.
     */
    public void clear() {
        resources.values().forEach(AllocatedResource::dispose);
        resources.clear();
    }

    /**
     * Sets the number of resources that passed in order for another attempt to be made
     * if a resource gets stolen. Resources can only be stolen in multithreaded renderings,
     * and even then it is very rare.
     *
     * <p>default=4</p>
     *
     * @param retryThreshold
     */
    public void setRetryThreshold(int retryThreshold) {
        this.retryThreshold = retryThreshold;
    }

    /**
     * Sets the number of frames resources last without being used before being
     * disposed by {@link #flush()}.
     *
     * @param timeoutLength
     */
    public void setTimeoutLength(int timeoutLength) {
        this.timeoutLength = timeoutLength;
    }

    /**
     * Gets the retry threshold.
     *
     * @return
     */
    public int getRetryThreshold() {
        return retryThreshold;
    }

    /**
     * Gets the timeout length.
     *
     * @return
     */
    public int getTimeoutLength() {
        return timeoutLength;
    }

    private static class AllocatedResource <T> implements ResourceWrapper<T> {

        private final long id;
        private final Disposer<T> disposer;
        private final AtomicBoolean acquired = new AtomicBoolean(false);
        private final BitSet reservedPositions = new BitSet();
        private final int timeoutLength;
        private T resource;
        private int timeout;

        public AllocatedResource(long id, int timeout, T resource, Disposer<T> disposer) {
            this.id = id;
            this.timeoutLength = this.timeout = timeout;
            this.resource = Objects.requireNonNull(resource, "Resource cannot be null.");
            this.disposer = disposer;
        }

        @Override
        public boolean acquire(int start, int end) {
            return isAvailable() && !isReserved(start, end) && !acquired.getAndSet(true);
        }

        @Override
        public T get() {
            return resource;
        }

        @Override
        public void release() {
            if (!acquired.getAndSet(false)) {
                throw new IllegalStateException("Resource was not acquired.");
            }
            timeout = timeoutLength;
        }

        @Override
        public void reserve(int position) {
            reservedPositions.set(position);
        }

        @Override
        public boolean isAvailable() {
            return resource != null && !acquired.get();
        }

        @Override
        public boolean isReserved(int start, int end) {
            if (reservedPositions.get(start)) {
                return false;
            }
            int i = reservedPositions.nextSetBit(start + 1);
            return i > start && i <= end;
        }

        public void dispose() {
            disposer.dispose(resource);
            resource = null;
        }

        public boolean cycle() {
            reservedPositions.clear();
            return timeout-- > 0;
        }

        public long getId() {
            return id;
        }

    }

}
