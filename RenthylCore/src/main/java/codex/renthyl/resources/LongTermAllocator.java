package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allocates resources intended for long-term use.
 *
 * <p>Allocated resources can never be re-acquired after the first release,
 * because the resource is assumed to be still in use. After release, the
 * resource is no longer tracked by this allocator, so it is possibly
 * elligible for garbage collection unless referenced elsewhere.</p>
 */
public class LongTermAllocator implements ResourceAllocator<ResourceWrapper> {

    @Override
    public ResourceWrapper allocate(ResourceDef def, int start, int end) {
        return new WeakWrapper(def.createResource());
    }

    private static class WeakWrapper <T> implements ResourceWrapper<T> {

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
        }

        @Override
        public void reserve(int position) {}

        @Override
        public boolean isAvailable() {
            return !acquired.get();
        }

        @Override
        public boolean isReserved(int start, int end) {
            return false;
        }

    }

}
