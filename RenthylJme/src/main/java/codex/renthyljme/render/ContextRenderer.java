package codex.renthyljme.render;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.render.Renderable;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;

/**
 * Renderable which requires access to {@link FrameGraphContext} to
 * perform rendering properly.
 */
public interface ContextRenderer extends Renderable {

    /**
     * Sets the upstream context socket to acquire {@link FrameGraphContext}
     * from during rendering.
     *
     * @param context
     */
    void setContext(Socket<? extends FrameGraphContext> context);

    /**
     * Gets the context socket that receives {@link FrameGraphContext} from
     * an {@link #setContext(Socket) upstream socket}.
     *
     * @return
     */
    PointerSocket<FrameGraphContext> getContext();

}
