package codex.renthyl.tasks;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.util.FBuffer;
import codex.renthyl.util.FrameBufferManager;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.texture.Texture2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PostProcessPass extends RenderTask {

    public static final String SCENE_COLOR = "Texture";
    public static final String SCENE_DEPTH = "DepthTexture";

    protected final Material material;
    protected final SocketMap<String, ValueSocket<Object>, Object> parameters = new SocketMap<>(this, new HashMap<>());
    protected final AllocationSocket<Texture2D> result;
    protected final FrameBufferManager buffers = new FrameBufferManager();

    public PostProcessPass(ResourceAllocator allocator, ResourceDef<Texture2D> def, Material material) {
        this.material = material;
        result = addSocket(new AllocationSocket<>(this, allocator, def));
        addSockets(parameters);
        parameters.put(SCENE_COLOR, new ValueSocket<>(this));
        parameters.put(SCENE_DEPTH, new ValueSocket<>(this));
    }

    @Override
    protected void renderTask() {
        Texture2D target = result.acquire();
        FBuffer fb = buffers.getFrameBuffer(target.getImage().getWidth(), target.getImage().getHeight(), 1);
        fb.setColorTargets(result.acquire());
        context.getFrameBuffer().pushValue(fb);
        context.clearBuffers();
        Map<String, Object> map = parameters.acquire();
        MaterialDef matdef = material.getMaterialDef();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (matdef.getMaterialParam(e.getKey()) != null) {
                // HashMap permits null values
                if (e.getValue() != null) {
                    material.setParam(e.getKey(), e.getValue());
                } else {
                    material.clearParam(e.getKey());
                }
            }
        }
        context.renderFullscreen(material);
        context.getFrameBuffer().pop();
    }

    @Override
    public void reset() {
        super.reset();
        buffers.flush();
    }

    public void setParameter(String name, Object value) {
        Objects.requireNonNull(parameters.get(name), "Parameter \"" + name + "\" does not exist.").setValue(value);
    }

    public PointerSocket<Map<String, Object>> getParameters() {
        return parameters;
    }

    public ValueSocket<Object> getParameter(String name) {
        return parameters.get(name);
    }

    public ValueSocket<Object> getColor() {
        return parameters.get(SCENE_COLOR);
    }

    public ValueSocket<Object> getDepth() {
        return parameters.get(SCENE_COLOR);
    }

    public PointerSocket<Texture2D> getResult() {
        return result;
    }

}
