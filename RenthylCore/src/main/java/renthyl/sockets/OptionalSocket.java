package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

/**
 * Socket which only operates when enabled.
 *
 * <p>This is particularly useful for tasks implementing interfaces that require
 * an additional input socket, but the task does not require it. A disabled
 * optional socket can be put in place to act as a fully-functional socket
 * to connect with, but it will only stage its upstream sockets if enabled.</p>
 *
 * @param <T>
 */
public class OptionalSocket <T> extends TransitiveSocket<T> {

    private boolean enabled;
    private boolean queued = false;

    /**
     * Creates an enabled OptionalSocket.
     *
     * @param task underlying task
     */
    public OptionalSocket(Renderable task) {
        this(task, true);
    }

    /**
     *
     * @param task underlying task
     * @param enabled initial enabled state
     */
    public OptionalSocket(Renderable task, boolean enabled) {
        super(task);
        this.enabled = enabled;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        queued = true;
        if (enabled) {
            super.stage(globals, queue);
        }
    }

    @Override
    public void reference(int queuePosition) {
        if (enabled) {
            super.reference(queuePosition);
        }
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return !enabled || super.isUpstreamAvailable(queuePosition);
    }

    @Override
    public T acquire() {
        return enabled ? super.acquire() : null;
    }

    @Override
    public void release(int queuePosition) {
        if (enabled) {
            if (--activeRefs < 0) {
                throw new IllegalStateException("More releases than references.");
            }
            if (upstream != null) {
                upstream.release(queuePosition);
            }
        }
    }

    @Override
    public void resetSocket() {
        super.resetSocket();
        queued = false;
    }

    @Override
    public T acquireOrThrow(String message) {
        if (!enabled) {
            throw new NullPointerException("Optional socket is disabled: " + message);
        }
        return super.acquireOrThrow(message);
    }

    /**
     * Enables or disables this socket.
     *
     * <p>If enabled, this functions the same as a {@link TransitiveSocket}.
     * If disabled, all functions are likewise disabled, including {@link #acquire()}
     * which returns null.</p>
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        if (queued) {
            throw new IllegalStateException("Cannot enable or disable when queued.");
        }
        this.enabled = enabled;
    }

    /**
     * Returns true if enabled.
     *
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }

}
