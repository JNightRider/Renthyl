package codex.renthyl.tasks;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MaterialPass extends RenderTask {

    protected final Material material;
    protected final SocketMap<String, ArgumentSocket<Object>, Object> parameters = new SocketMap<>(this, new HashMap<>());
    private final AllocationSocket<Texture2D> result;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final Camera camera = new Camera(1024, 1024);

    public MaterialPass(ResourceAllocator allocator, Material material, ResourceDef<Texture2D> resultDef) {
        this.material = material;
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        addSockets(parameters);
        ArgumentSocket<Texture2D> tex = new ArgumentSocket<>(this);
        parameters.put("myParam", tex);
    }

    @Override
    protected void renderTask() {

        // camera
        Texture target = result.acquire();
        context.getCamera().pushResize(camera, target.getImage().getWidth(), target.getImage().getHeight(), false);

        // framebuffer
        bufferDef.setColorTargets(target);
        FrameBuffer fb = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fb);
        context.clearBuffers();

        // material
        Map<String, ?> map = parameters.acquire();
        MaterialDef matdef = material.getMaterialDef();
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (matdef.getMaterialParam(e.getKey()) != null) {
                // HashMap permits null values
                if (e.getValue() != null) {
                    material.setParam(e.getKey(), e.getValue());
                } else {
                    material.clearParam(e.getKey());
                }
            }
        }

        // render
        context.renderFullscreen(material);

        // reset settings
        context.getFrameBuffer().pop();
        context.getCamera().pop();

    }

    public void setParameter(String name, Object value) {
        Objects.requireNonNull(parameters.get(name), "Parameter \"" + name + "\" does not exist.").setValue(value);
    }

    public PointerSocket<? extends Map<String, ?>> getParameters() {
        return parameters;
    }

    public ArgumentSocket getParameter(String name) {
        return parameters.get(name);
    }

    public AllocationSocket<Texture2D> getResult() {
        return result;
    }

}
