package codex.renthyl.tasks;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.ContextRenderer;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;

public abstract class RenderTask extends AbstractTask implements ContextRenderer {

    protected final TransitiveSocket<FrameGraphContext> contextSocket = new TransitiveSocket<>(this);
    protected FrameGraphContext context;

    public RenderTask() {
        addSocket(contextSocket);
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        if (position < QUEUING && contextSocket.getUpstream() == null) {
            contextSocket.setUpstream(globals.get(FrameGraphContext.CONTEXT_GLOBAL));
        }
    }

    @Override
    public void render() {
        context = contextSocket.acquireOrThrow(getClass().getName() + " requires context.");
        super.render();
        context = null; // set back to null so that context is used at the expected time only
    }

    @Override
    public void setContext(Socket<? extends FrameGraphContext> context) {
        contextSocket.setUpstream(context);
    }

    @Override
    public PointerSocket<FrameGraphContext> getContext() {
        return contextSocket;
    }

}
