package codex.renthyl.resources;

/**
 * Safely disposes of objects.
 *
 * @param <T>
 */
public interface Disposer <T> {

    /**
     * Disposes {@code object}.
     *
     * @param object
     */
    void dispose(T object);

}
