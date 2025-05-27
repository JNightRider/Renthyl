/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.boost.material.MaterialAdapter;
import codex.renthyl.FrameGraphContext;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.geometry.GeometryRenderHandler;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.AllocationSocketMap;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.collections.SocketMap;
import codex.renthyl.tasks.RenderTask;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * Renders direct lighting for the scene and constructs geometry
 * buffers suitable for indirect lighting calculations.
 * 
 * @author codex
 */
public class DirectLightingPass extends RenderTask implements GeometryRenderHandler {
    
    public static final String TECHNIQUE = "VXGI_DirectLighting";
    private static final MaterialAdapter adapter = new MaterialAdapter();
    private static final String[] GBUFFER_ORDER = {"Color", "Diffuse", "Position", "Normals", "Material"};
    
    static {
        adapter.add("Common/MatDefs/Light/PBRLighting.j3md", "RenthylPlus/MatDefs/VXGI/pbrDirect.j3md");
    }

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<float[]> lights = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> lightContribution = new TransitiveSocket<>(this);
    private final AllocationSocketMap<String, TextureDef<Texture2D>, Texture2D> gbufferMap = new AllocationSocketMap<>(this);
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final Vector2f screenSize = new Vector2f();

    public DirectLightingPass(ResourceAllocator allocator) {
        addSockets(geometry, lights, lightContribution, gbufferMap);
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        gbufferMap.put("Color", allocator, TextureDef.texture2D(Image.Format.RGBA16F));
        gbufferMap.put("Depth", allocator, TextureDef.texture2D(Image.Format.Depth16));
        gbufferMap.put("Diffuse", allocator, TextureDef.texture2D(Image.Format.RGBA16F));
        gbufferMap.put("Position", allocator, TextureDef.texture2D(Image.Format.RGBA16F));
        gbufferMap.put("Normals", allocator, TextureDef.texture2D(Image.Format.RGBA16F));
        gbufferMap.put("Material", allocator, TextureDef.texture2D(Image.Format.RGBA32F));
    }

    @Override
    protected void renderTask() {

        // configure definitions
        screenSize.set(context.getWidth(), context.getHeight());
        for (TextureDef<Texture2D> d : gbufferMap.getDefs().values()) {
            d.setSize(context.getWidth(), context.getHeight());
        }

        // configure framebuffer
        bufferDef.setColorTargets(gbufferMap.acquireArray(Texture2D[]::new, GBUFFER_ORDER));
        bufferDef.setDepthTarget(gbufferMap.get("Depth").acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        fbo.setMultiTarget(true);
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        // render
        context.getForcedTechnique().pushValue(TECHNIQUE);
        for (GeometryQueue q : geometry.acquire()) {
            q.render(context, this);
        }

        // restore settings
        context.getForcedTechnique().pop();
        context.getFrameBuffer().pop();

    }

    @Override
    public void renderGeometry(FrameGraphContext context, Geometry g) {
        Material m = g.getMaterial();
        if (!adapter.adaptMaterial(context.getAssetManager(), m, TECHNIQUE)) {
            return;
        }
        float[] lightData = lights.acquireOrThrow("Light data required.");
        m.setInt("VXGI_LightDataSize", lightData.length);
        m.setParam("VXGI_LightData", VarType.FloatArray, lightData);
        m.setTexture("VXGI_LightContributionMap", lightContribution.acquire());
        m.setVector2("VXGI_ScreenSize", screenSize);
        context.getRenderManager().renderGeometry(g);
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public PointerSocket<float[]> getLights() {
        return lights;
    }

    public PointerSocket<Texture2D> getLightContribution() {
        return lightContribution;
    }

    public SocketMap<String, ? extends Socket<Texture2D>, Texture2D> getGBufferMap() {
        return gbufferMap;
    }

}
