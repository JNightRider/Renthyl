package codex.renthyl.tasks;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;

public abstract class RenderTask extends AbstractTask {

    private final TransitiveSocket<FrameGraphContext> contextSocket = new TransitiveSocket<>(this);
    protected FrameGraphContext context;

    public RenderTask() {
        addSocket(contextSocket);
    }

    @Override
    public void render() {
        context = contextSocket.acquireOrThrow(getClass().getName() + " requires context.");
        super.render();
    }

    public void setContext(Socket<FrameGraphContext> context) {
        contextSocket.setUpstream(context);
    }

    public PointerSocket<FrameGraphContext> getContext() {
        return contextSocket;
    }

}
