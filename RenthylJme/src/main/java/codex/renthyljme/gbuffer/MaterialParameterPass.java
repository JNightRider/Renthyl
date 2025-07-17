package codex.renthyljme.gbuffer;

import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.collections.SocketMap;
import codex.renthyl.tasks.AbstractTask;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.utils.MaterialUtils;
import com.jme3.scene.Geometry;

import java.util.List;
import java.util.Map;

public class MaterialParameterPass extends AbstractTask {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final SocketMap<String, ArgumentSocket<Object>, Object> parameters = new SocketMap<>(this);

    public MaterialParameterPass() {
        addSockets(geometry, parameters);
    }

    @Override
    protected void renderTask() {
        List<GeometryQueue> queues = geometry.acquire();
        Map<String, Object> params = parameters.acquire();
        for (Map.Entry<String, Object> e : params.entrySet()) {
            for (GeometryQueue q : queues) {
                for (Geometry g : q) {
                    MaterialUtils.setIfExists(g.getMaterial(), e.getKey(), e.getValue());
                }
            }
        }
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public ArgumentSocket<Object> getParameter(String name) {
        ArgumentSocket<Object> p = parameters.get(name);
        if (p == null) {
            p = new ArgumentSocket<>(this);
            parameters.put(name, p);
        }
        return p;
    }

}
