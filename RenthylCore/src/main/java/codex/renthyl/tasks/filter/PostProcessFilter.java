package codex.renthyl.tasks.filter;

import codex.renthyl.render.Renderable;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import com.jme3.texture.Texture2D;

public interface PostProcessFilter extends Renderable {

    PointerSocket<Texture2D> getSceneColor();

    PointerSocket<Texture2D> getSceneDepth();

    Socket<Texture2D> getFilterResult();

}
