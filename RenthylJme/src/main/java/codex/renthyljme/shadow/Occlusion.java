package codex.renthyljme.shadow;

import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.render.Renderable;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import com.jme3.light.Light;

import java.util.Collection;

public interface Occlusion <T extends Light> extends Renderable {

    ArgumentSocket<T> getLight();

    PointerSocket<GeometryQueue> getOccluders();

    PointerSocket<GeometryQueue> getReceivers();

    Socket<? extends Collection<ShadowMap>> getShadowMaps();

}
