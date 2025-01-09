/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules.geometry;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class GeometryDepthPass extends RenderPass {

    public static final String TECHNIQUE = "Depth";
    private static ImmediateMatDef matdef;
    
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<Texture2D> depth;
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private Material material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        depth = addOutput("Depth");
        if (matdef == null) {
            ImmediateShader vert = new ImmediateShader(Shader.ShaderType.Vertex)
                    .includeGlslCompat()
                    .includeInstancing()
                    .includeSkinning()
                    .includeMorphing()
                    .attribute("vec3", "inPosition")
                    .main()
                        .assign("vec4", "modelSpacePos", "vec4(inPosition, 1.0)")
                        .ifdef("NUM_MORPH_TARGETS")
                            .call("Morph_Compute", "modelSpacePos")
                        .endif()
                        .ifdef("NUM_BONES")
                            .call("Skinning_Compute", "ModelSpacePos")
                        .endif()
                        .assign("gl_Position", "TransformWorldViewProjection(modelSpacePos)")
                    .end();
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment)
                    .includeGlslCompat()
                    .main()
                        .assign("gl_FragColor", "vec4(1.0)")
                    .end();
            matdef = new ImmediateMatDef(frameGraph.getAssetManager(), "Depth")
                    .addParam(VarType.Int, "BoundDrawBuffer")
                    .addParam(VarType.Boolean, "UseInstancing")
                    .addParam(VarType.Int, "NumberOfBones")
                    .addParam(VarType.Matrix4Array, "BoneMatrices")
                    .addParam(VarType.FloatArray, "MorphWeights")
                    .addParam(VarType.Int, "NumberOfMorphTargets")
                    .addParam(VarType.Int, "NumberOfTargetsBuffers");
            matdef.createTechnique()
                    .setVersions(310, 300, 150, 100)
                    .setShader(vert).setShader(frag)
                    .addWorldParameters("WorldViewProjectionMatrix")
                    .addDefine("BOUND_DRAW_BUFFER", "BoundDrawBuffer")
                    .addDefine("INSTANCING", "UseInstancing")
                    .addDefine("NUM_BONES", "NumberOfBones")
                    .addDefine("NUM_MORPH_TARGETS", "NumberOfMorphTargets")
                    .addDefine("NUM_TARGETS_BUFFERS", "NumberOfTargetsBuffers")
                    .add();
        }
        material = matdef.createMaterial();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(depthDef, depth);
        reserve(depth);
        reference(geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        depthDef.setSize(context.getWidth(), context.getHeight());
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireDepthTarget(fb, depth);
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        context.registerMode(RenderMode.forcedTechnique(TECHNIQUE));
        context.registerMode(RenderMode.forcedMaterial(material));
        resources.acquire(geometry).render(context, GeometryRenderHandler.DEFAULT);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
