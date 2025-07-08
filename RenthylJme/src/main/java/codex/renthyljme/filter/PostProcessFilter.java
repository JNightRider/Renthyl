package codex.renthyljme.filter;

import codex.renthyl.render.Renderable;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import com.jme3.texture.Texture2D;

/**
 * Renderable task designed to perform post-processing filter.
 *
 * @author codex
 */
public interface PostProcessFilter extends Renderable {

    /**
     * Gets socket for scene color (input).
     *
     * @return
     */
    PointerSocket<Texture2D> getSceneColor();

    /**
     * Gets socket for scene depth (input).
     *
     * @return
     */
    PointerSocket<Texture2D> getSceneDepth();

    /**
     * Gets socket for the post-processing result (output).
     *
     * @return
     */
    Socket<Texture2D> getFilterResult();

}
