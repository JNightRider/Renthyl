package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

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
