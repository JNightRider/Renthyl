package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;

/**
 * Allocates resources on request as dictated by {@link ResourceDef resource definitions}.
 *
 * <p>The primary goal of implementors should be to minimize the number of resources that must
 * be created in order to satisfy each {@link #allocate(ResourceDef, int, int) allocate} call.
 * In other words, existing resources should be reallocated whenever deemed possible.</p>
 *
 * <p>It is critical that a resource not be allocated to two different requesters at once. When
 * allocated by a ResourceAllocator, the resource must be {@link ResourceWrapper#isAvailable() available}.</p>
 *
 * @param <T>
 */
public interface ResourceAllocator <T extends ResourceWrapper> {

    /**
     * Allocates an approved wrapped resource.
     *
     * <p>The returned {@link ResourceWrapper} will already be {@link ResourceWrapper#acquire(int, int) acquired}
     * on return. The wrapper cannot be allocated to any other process until the wrapper is released. If the resource
     * definition does not approve any existing resources, a new resource will be created using the resource definition.
     * The new resource will be made available for allocation for other processes.</p>
     *
     * @param def
     * @param start queue position from which a resource is being requested
     * @param end queue position from which the resource will presumably be
     *            released (very helpful in many cases, but not critical)
     * @return allocated wrapped resource
     */
    T allocate(ResourceDef def, int start, int end);

    /**
     * Retrieves the wrapper of {@code resource} if the wrapper exists in this allocator.
     *
     * @param resource resource to acquire
     * @return wrapper of {@code resource}
     */
    T getWrapperOf(Object resource);

}
