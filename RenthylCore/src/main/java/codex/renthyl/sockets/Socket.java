package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.render.queue.Stageable;
import codex.renthyl.render.Referenceable;

import java.util.Objects;

/**
 * Acts as a connecting point between a {@link Renderable renderable task} and other tasks in the framegraph,
 * through which resources can be shared.
 *
 * <p>Sockets are designed to fit the lifecycle of a render frame, and therefore must be handled in that way.</p>
 *
 * @param <T> type of resource that may be shared
 * @author codex
 */
public interface Socket <T> extends Stageable, Referenceable {

    /**
     * Updates this socket.
     *
     * <p>This is expected to be performed after {@link Stageable#stage(GlobalAttributes, RenderingQueue) staging}
     * is completed.</p>
     *
     * @param tpf time in seconds per frame
     */
    void update(float tpf);

    /**
     * Determines if this socket's resource is available for access by downstream sockets.
     *
     * <p>If true, the underlying task is expected to have completed all operations associated
     * with the underlying resource. This is usually determined by {@link Renderable#isRenderingComplete()},
     * as all resources should be released at that point. Additionally, this socket's upstream
     * sockets (if applicable) are also expected to be available to downstream.</p>
     *
     * @param queuePosition position from which the availability query originated
     * @return true if available
     */
    boolean isAvailableToDownstream(int queuePosition);

    /**
     * Queries the {@link #isAvailableToDownstream(int) availability} of upstream socket(s),
     * if they exist, usually to determine if the underlying task of this socket may be
     * executed.
     *
     * @param queuePosition position from which the availability query originated
     * @return true if all upstream sockets are available or no upstream sockets exist
     */
    boolean isUpstreamAvailable(int queuePosition);

    /**
     * Returns the underlying resource of this socket. Underlying resource refers to a
     * resource directly stored by this socket, or else the resource returned by an upstream
     * socket (if applicable). The exact value to return is up to the specific implementation.
     *
     * <p>It is expected that the caller have {@link Referenceable#reference(int) referenced}
     * this socket during the {@link Renderable#prepare() preparation} step, but not yet
     * {@link Referenceable#release(int) released} this socket.</p>
     *
     * @return the underlying resource
     */
    T acquire();

    /**
     * Resets this socket after all rendering has been completed.
     *
     * <p>If not all {@link #reference(int) references} have been {@link #release(int) released},
     * implementations are encouraged to throw an exception.</p>
     */
    void resetSocket();

    /**
     * Returns the computed usage of the underlying resource. The usage is computed
     * by the maximum of the {@link #getActiveReferences() active references} to
     * this socket and the resource usage of any given upstream socket (if applicable).
     *
     * @return resource usage
     */
    int getResourceUsage();

    /**
     * Acquires the underlying resource, or returns {@code orElse} if the resource is null.
     *
     * @param orElse default value to return if
     * @return underying resource, or {@code orElse}
     * @see #acquire()
     */
    default T acquire(T orElse) {
        T resource = acquire();
        return resource != null ? resource : orElse;
    }

    /**
     * Acquires the underlying resource. If the resource is null, an exception is thrown.
     *
     * @return underlying resource
     * @see #acquire()
     */
    default T acquireOrThrow() {
        return acquireOrThrow("Unable to acquire.");
    }

    /**
     * Acquires the underlying resource. If the resource is null, an exception
     * with {@code message} is returned.
     *
     * @param message exception message
     * @return underlying resource
     * @see #acquire()
     */
    default T acquireOrThrow(String message) {
        return Objects.requireNonNull(acquire(), message);
    }

}
