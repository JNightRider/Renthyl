package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;

/**
 * Handles acquiring and releasing of a shared resource.
 *
 * @param <T>
 */
public interface ResourceWrapper <T> {

    /**
     * Requests to acquire this wrapper.
     *
     * @param start queue position from where the request is being made
     * @param end queue position from where the wrapper will presumably be released
     *            (very helpful, but not critical in most cases)
     * @return true if the wrapper accepts the request
     */
    boolean acquire(int start, int end);

    /**
     * Gets the wrapped resource.
     *
     * @return
     */
    T get();

    /**
     * Releases the wrapper from a successful acquire call.
     */
    void release();

    /**
     * Returns true if this wrapper is not currently acquired.
     *
     * @return
     */
    boolean isAvailable();

    static <W extends ResourceWrapper<T>, T> ResourceWrapper<T> acquire(ResourceAllocator<W> allocator, W current, ResourceDef<T> def, int start, int end) {
        if (current == null || !current.isAvailable() || def.evaluateResource(current.get()) == null || !current.acquire(start, end)) {
            return allocator.allocate(def, start, end);
        }
        return current;
    }

}
