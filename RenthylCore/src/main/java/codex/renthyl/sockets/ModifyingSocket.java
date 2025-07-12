package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

/**
 * Socket which only allows the underlying task to run once all other tasks
 * are finished with the underlying resource. Like this name suggests, this
 * behavior allows the underlying task to modify the incoming resource, as
 * all other tasks are finished with it.
 *
 * <p>This is a rather risky socket type to use. It can potentially slow down
 * rendering, particularly in multithreaded situations, and it can easily lead
 * to deadlocking if multiple modifying sockets are waiting for the same resource
 * to become available. In most cases, it is better to use a different socket type.</p>
 *
 * @param <T>
 * @author codex
 */
public class ModifyingSocket <T> extends TransitiveSocket<T> {

    public ModifyingSocket(Renderable task) {
        super(task);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || (upstream.isAvailableToDownstream(queuePosition)
                && upstream.getResourceUsage() == getActiveReferences());
    }

}
