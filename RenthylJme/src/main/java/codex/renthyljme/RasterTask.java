package codex.renthyljme;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.AbstractTask;

public abstract class RasterTask extends AbstractTask {

    protected final TransitiveSocket<FrameGraphContext> contextSocket = new TransitiveSocket<>(this);
    protected FrameGraphContext context; // easy handle for subclasses to access the context during render

    public RasterTask() {
        addSocket(contextSocket);
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        if (!prestaged && contextSocket.getUpstream() == null) {
            contextSocket.setUpstream(globals.get(FrameGraphContext.CONTEXT_GLOBAL));
        }
        super.preStage(globals);
    }

    @Override
    public void render() {
        context = contextSocket.acquireOrThrow(getClass().getName() + " requires context.");
        super.render();
        context = null; // set back to null so that context is used at the expected time only
    }

}
