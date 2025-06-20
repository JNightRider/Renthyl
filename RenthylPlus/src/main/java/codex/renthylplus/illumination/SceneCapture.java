package codex.renthylplus.illumination;

import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.Frame;
import codex.renthyl.tasks.utils.Storage;
import com.jme3.math.Vector3f;

import java.util.HashMap;

public class SceneCapture extends Frame {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final HashMap<Vector3f, DirectionalCapture> passes = new HashMap<>();
    private final Storage<Vector3f, CaptureMap> captures = new Storage<>();

    public SceneCapture() {
        addSockets(geometry);
    }

}
