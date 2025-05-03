package codex.renthyl.newresources;

public class ModifyingSocket <T> extends TransitSocket<T> {

    public ModifyingSocket(Renderable task) {
        super(task);
    }

    @Override
    public boolean isUpstreamAvailable() {
        return upstream == null || (upstream.isAvailableToDownstream()
                && getUpstreamRoot().getActiveReferences() == getActiveReferences());
    }

}
