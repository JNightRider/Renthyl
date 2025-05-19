package codex.renthyl.tasks;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;

public abstract class RenderTask extends AbstractTask {

    protected final TransitiveSocket<FrameGraphContext> contextSocket = new TransitiveSocket<>(this);
    protected FrameGraphContext context;

    public RenderTask() {
        addSocket(contextSocket);
    }

    @Override
    public void render() {
        context = contextSocket.acquireOrThrow(getClass().getName() + " requires context.");
        super.render();
        context = null; // set back to null so that context is used at the expected time only
    }

    public void setContext(Socket<FrameGraphContext> context) {
        contextSocket.setUpstream(context);
    }

    public PointerSocket<FrameGraphContext> getContext() {
        return contextSocket;
    }

}
