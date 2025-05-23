package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;

public class OptionalSocket <T> extends TransitiveSocket<T> {

    private boolean enabled;
    private boolean queued = false;

    public OptionalSocket(Renderable task) {
        this(task, true);
    }

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

    public void setEnabled(boolean enabled) {
        if (queued) {
            throw new IllegalStateException("Cannot enable or disable when queued.");
        }
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
