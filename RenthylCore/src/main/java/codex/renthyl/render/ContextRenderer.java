package codex.renthyl.render;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;

public interface ContextRenderer extends Renderable {

    void setContext(Socket<? extends FrameGraphContext> context);

    PointerSocket<FrameGraphContext> getContext();

}
