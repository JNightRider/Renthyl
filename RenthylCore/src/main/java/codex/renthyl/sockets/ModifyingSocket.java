package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

public class ModifyingSocket <T> extends TransitiveSocket<T> {

    public ModifyingSocket(Renderable task) {
        super(task);
    }

    @Override
    public boolean isUpstreamAvailable() {
        return upstream == null || (upstream.isAvailableToDownstream()
                && upstream.getResourceUsage() == getActiveReferences());
    }

}
