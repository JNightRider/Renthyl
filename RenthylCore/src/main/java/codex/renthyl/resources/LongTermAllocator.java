package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allocates resources intended for long-term use.
 *
 * <p>Allocated resources can never be re-acquired after the first release,
 * because the resource is assumed to be still in use. After release, the
 * resource is no longer tracked by this allocator, so it is possibly
 * elligible for garbage collection unless referenced elsewhere.</p>
 */
@Deprecated
public class LongTermAllocator implements ResourceAllocator<ResourceWrapper> {

    private final Collection<WeakWrapper> wrappers = new ArrayList<>();

    @Override
    public ResourceWrapper allocate(ResourceDef def, int start, int end) {
        WeakWrapper w = new WeakWrapper(def.createResource());
        wrappers.add(w);
        return w;
    }

    @Override
    public ResourceWrapper getWrapperOf(Object resource) {
        for (WeakWrapper w : wrappers) {
            if (w.get() == resource) {
                return w;
            }
        }
        return null;
    }

    private class WeakWrapper <T> implements ResourceWrapper<T> {

        private T resource;
        private final AtomicBoolean acquired = new AtomicBoolean(false);

        private WeakWrapper(T resource) {
            this.resource = resource;
        }

        @Override
        public boolean acquire(int start, int end) {
            return !acquired.getAndSet(true);
        }

        @Override
        public T get() {
            return resource;
        }

        @Override
        public void release() {
            if (!acquired.get()) {
                throw new IllegalStateException("Must acquire before releasing.");
            }
            resource = null; // oopsie, lost the resource
            wrappers.remove(this);
        }

        @Override
        public boolean isAvailable() {
            return !acquired.get();
        }

    }

}
