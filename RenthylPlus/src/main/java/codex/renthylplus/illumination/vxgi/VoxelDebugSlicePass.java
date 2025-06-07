/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.illumination.vxgi;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.RenderTask;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class VoxelDebugSlicePass extends RenderTask {
    
    private static ImmediateMatDef matdef;

    private final TransitiveSocket<Texture3D> voxels = new TransitiveSocket<>(this);
    private final AllocationSocket<Texture2D> result;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private final TextureDef<Texture3D> voxelDef = TextureDef.texture3D(Image.Format.RGBA8);
    private Material material;
    private float slice = 0;

    public VoxelDebugSlicePass(ResourceAllocator allocator) {
        addSocket(voxels);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
    }

    @Override
    protected void renderTask() {

        if (material == null) {
            createMaterial();
        }

        resultDef.setSize(context.getWidth(), context.getHeight());

        bufferDef.setColorTargets(result.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        material.setTexture("VoxelMap", voxels.acquireOrThrow());
        material.setFloat("Slice", slice);
        context.renderFullscreen(material);

        slice += context.getTpf();
        if (slice >= 1.0) {
            slice = 0;
        }

    }

    private void createMaterial() {
        if (matdef == null) {
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment, true)
                    .includeGlslCompat()
                    .uniform("sampler3D", "m_VoxelMap")
                    .uniform("float", "m_Slice")
                    .varying("vec2", "texCoord")
                    .main()
                    //.assign("ivec3", "size", "imageSize(m_VoxelMap)")
                    .assign("gl_FragColor", "texture3D(m_VoxelMap, vec3(texCoord, m_Slice))")
                    //.assign("gl_FragColor", "imageLoad(m_VoxelMap, ivec3(size * vec3(texCoord, m_Slice)) + size)")
                    //.assign("gl_FragColor", "vec4(texCoord, m_Slice, 1.0)")
                    .end();
            matdef = new ImmediateMatDef(context.getAssetManager(), "VoxelSliceVis")
                    .addParam(VarType.Texture3D, "VoxelMap")
                    .addParam(VarType.Float, "Slice", 0f);
            matdef.createTechnique()
                    .setVersions(450, 430)
                    .setVertexShader("RenthylCore/MatDefs/Fullscreen/Screen.vert")
                    .setShader(frag)
                    .add();
        }
        material = matdef.createMaterial();
    }
    
}
