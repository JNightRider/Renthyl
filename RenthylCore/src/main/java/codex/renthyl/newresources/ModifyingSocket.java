package codex.renthyl.newresources;

public class ModifyingSocket <T> extends BasicSocket<T> {

    public ModifyingSocket(RenderTask task) {
        super(task);
    }

    @Override
    public boolean isUpstreamAvailable() {
        return upstream == null || (upstream.isAvailableToDownstream()
                && getUpstreamRoot().getActiveReferences() == getActiveReferences() + 1);
    }

}
