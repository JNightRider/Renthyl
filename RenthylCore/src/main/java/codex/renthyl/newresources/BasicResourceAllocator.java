package codex.renthyl.newresources;

import codex.renthyl.definitions.ResourceDef;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicResourceAllocator implements ResourceAllocator<ResourceWrapper> {

    private final Map<Long, AllocatedResource> resources = new ConcurrentHashMap<>();
    private long nextId = 0;
    private int retryThreshold = 4;
    private int timeoutLength = 1;

    @Override
    public ResourceWrapper allocate(ResourceDef def, int start, int end) {
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

    @Override
    public void flush() {
        for (Iterator<AllocatedResource> it = resources.values().iterator(); it.hasNext();) {
            AllocatedResource res = it.next();
            if (!res.cycle()) {
                res.dispose();
                it.remove();
            }
        }
    }

    public void setRetryThreshold(int retryThreshold) {
        this.retryThreshold = retryThreshold;
    }

    public void setTimeoutLength(int timeoutLength) {
        this.timeoutLength = timeoutLength;
    }

    public int getRetryThreshold() {
        return retryThreshold;
    }

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
            this.resource = resource;
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
