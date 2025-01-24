/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.plex;

import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.Glsl;
import codex.jmecompute.opengl.uniforms.bufferobjects.VertexBufferShaderStorage;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DefinedTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.resources.tickets.TicketSelector;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;

/**
 *
 * @author codex
 */
public class PlexGeometryPass extends RenderPass implements GeometryRenderHandler {
    
    private ResourceTicket<GeometryQueue> geometry;
    private DefinedTicketList<Texture2D, TextureDef<Texture2D>> outList;
    private GLComputeShader plexShader;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        outList.add("Color", TextureDef.texture2D());
        outList.add("Depth", TextureDef.texture2D(Image.Format.Depth));
        plexShader = UniversalShaderLoader.loadComputeShader(frameGraph.getAssetManager(),
                "RenthylPlus/MatDefs/Plex/Unshaded.glsl", Glsl.V430);
        plexShader.uniform(new VertexBufferShaderStorage("inIndex"));
        plexShader.uniform(new VertexBufferShaderStorage("inPosition"));
        plexShader.uniform(new VertexBufferShaderStorage("inTexCoord"));
        plexShader.uniform(new VertexBufferShaderStorage("inNormal"));
        plexShader.uniform(new VertexBufferShaderStorage("inTangent"));
        plexShader.uniformDefine("TANGENTS", "inTangent");
        plexShader.uniformFloat("Metallic").set(1.0f);
        plexShader.uniformFloat("Roughness").set(1.0f);
        plexShader.uniformFloat("EmissivePower").set(3.0f);
        plexShader.uniformFloat("EmissiveIntensity").set(2.0f);
        plexShader.uniformFloat("NormalType").set(-1.0f);
        plexShader.uniformFloat("Glossiness").set(1.0f);
        plexShader.uniformVector4("BaseColor").set(Vector4f.UNIT_XYZW);
        plexShader.uniformVector4("Specular").set(Vector4f.UNIT_XYZW);
        plexShader.uniformsTexture("BaseColorMap", "NormalMap",  "EmissiveMap");
        plexShader.uniformTexture("MetallicMap").setColorSpace(ColorSpace.Linear);
        plexShader.uniformTexture("RoughnessMap").setColorSpace(ColorSpace.Linear);
        plexShader.uniformTexture("ParallaxMap").setColorSpace(ColorSpace.Linear);
        plexShader.uniformsVector4("Emissive");
        plexShader.uniformBoolean("UseSpecularAA").set(true);
        plexShader.uniformBoolean("PackedNormalParallax");
        plexShader.uniformFloat("ParallaxHeight").set(0.05f);
        plexShader.uniformBoolean("HorizonFade");
        plexShader.uniformTexture("LightMap");
    }
    @Override
    protected TicketGroup createMainOutputGroup(String name) {
        return (outList = new DefinedTicketList<>(name));
    }
    @Override
    protected void prepare(FGRenderContext context) {
        outList.declareAll(resources, this);
        reserve(outList);
        reference(geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, outList.select(TicketSelector.name("Color")));
        resources.acquireDepthTarget(fb, outList.select(TicketSelector.name("Depth")));
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        
        resources.acquire(geometry).render(context, this);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void renderGeometry(FGRenderContext context, Geometry geometry) {
        if (geometry.getMesh() instanceof PlexMesh) {
            renderPlexGeometry(context, geometry);
        } else {
            context.getRenderManager().renderGeometry(geometry);
        }
    }
    
    private void renderPlexGeometry(FGRenderContext context, Geometry geometry) {
        Camera cam = context.getCurrentCamera();
        PlexMesh mesh = (PlexMesh)geometry.getMesh();
        float dist = geometry.getWorldBound().distanceToEdge(cam.getLocation());
        int limit = mesh.selectRasterLimit(context.getCurrentCamera(), dist);
        context.getRenderManager().renderGeometry(geometry);
    }
    
}
