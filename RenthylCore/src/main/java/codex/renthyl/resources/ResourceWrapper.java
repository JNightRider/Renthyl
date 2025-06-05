package codex.renthyl.resources;

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
     * Reserves this wrapper at {@code queuePosition}.
     *
     * <p>The wrapper takes reservations into consideration when evaluating
     * acquire requests. This is not guaranteed to make this wrapper available
     * at the reserved position.</p>
     *
     * @param position
     */
    void reserve(int position);

    /**
     * Returns true if this wrapper is not currently acquired.
     *
     * @return
     */
    boolean isAvailable();

    /**
     * Returns true if this wrapper is reserved at any point between
     * {@code start} and {@code end} (inclusive).
     *
     * @param start
     * @param end
     * @return
     */
    boolean isReserved(int start, int end);

    /**
     * Returns true if this wrapper is both {@link #isAvailable()} and not reserved
     * at any point between {@code start} and {@code end} (inclusive).
     *
     * @param start
     * @param end
     * @return
     */
    default boolean isAvailable(int start, int end) {
        return isAvailable() && !isReserved(start, end);
    }

}
