package codex.renthyl.render;

/**
 * Restricts certain operations to between a {@link #reference(int) reference} and corresponding
 * {@link #release(int) release}.
 */
public interface Referenceable {

    /**
     * References this object from {@code queuePosition} in a
     * {@link codex.renthyl.render.queue.RenderingQueue RenderingQueue}.
     *
     * <p>All reference calls <em>must</em> be followed a corresponding {@link #release(int) release}
     * call at some point. Implementations may, at their descretion, assert that no references are active
     * (that is, closed by a release call).</p>
     *
     * @param queuePosition position in the queue from where the reference is originating
     */
    void reference(int queuePosition);

    /**
     * Releases a corresponding {@link #reference(int) reference} from the same {@code queuePosition}.
     *
     * @param queuePosition position in the queue from which the reference to close originated
     */
    void release(int queuePosition);

    /**
     * Returns the number of active references (those not closed by corresponding
     * {@link #release(int)} calls).
     *
     * @return number of active references
     */
    int getActiveReferences();

    /**
     * Asserts that no active references exist on this object.
     *
     * @throws IllegalStateException if an active reference exists
     */
    default void assertNoActiveReferences() {
        if (getActiveReferences() > 0) {
            throw new IllegalStateException("Cannot have active references during this operation.");
        }
    }

}
