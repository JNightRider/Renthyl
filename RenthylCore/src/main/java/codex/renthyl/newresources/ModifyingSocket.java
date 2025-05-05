package codex.renthyl.newresources;

public class ModifyingSocket <T> extends TransitiveSocket<T> {

    public ModifyingSocket(Renderable task) {
        super(task);
    }

    @Override
    public boolean isUpstreamAvailable() {
        return upstream == null || (upstream.isAvailableToDownstream()
                && getUpstreamTerminal().getActiveReferences() == getActiveReferences());
    }

}
