package codex.renthylplus.gbuffer;

import codex.jmecompute.Stride;
import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.uniforms.GLUniform;
import codex.jmecompute.opengl.uniforms.buffers.FloatArrayUniform;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.AbstractTask;
import com.jme3.asset.AssetManager;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GBufferLightingPass extends AbstractTask {

    private final SocketList<? extends Socket<Texture2D>, Texture2D> gbuffers = new SocketList<>(this);
    private final TransitiveSocket<Texture2D> depth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Camera> camera = new TransitiveSocket<>(this);
    private final TransitiveSocket<float[]> lightData = new TransitiveSocket<>(this);
    private final AllocationSocket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private final WorkSize work = new WorkSize();
    private final GLComputeShader shader;
    private final Matrix4f viewProjInverse = new Matrix4f();
    private final Collection<String> bufferSlots = new ArrayList<>();
    private TextureImage resultImage;

    public GBufferLightingPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(gbuffers, depth, camera, lightData);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylPlus/MatDefs/GBuffers/GBufferPBR.glsl");
        shader.uniformMatrix4("ViewProjectionInverse");
        shader.uniformVector3("CameraPosition");
        shader.uniform(new FloatArrayUniform("LightData", Stride.Vec4));
        shader.uniformImage("ResultImage");
    }

    @Override
    protected void renderTask() {

        Camera cam = camera.acquireOrThrow("Camera required.");
        float[] lights = lightData.acquireOrThrow("Light data required.");
        cam.getViewProjectionMatrix().invert(viewProjInverse);

        shader.set("ViewProjectionInverse", viewProjInverse);
        shader.set("CameraPosition", cam.getLocation());
        shader.set("LightData", lights);
        shader.define("LIGHT_DATA_LENGTH", lights.length / 4);

        int slot = 1;
        List<Texture2D> gbufs = gbuffers.acquire();
        for (Texture2D g : gbufs) {
            String name = "m_GBuffer" + slot;
            GLUniform u = shader.getUniform(name);
            if (u == null) {
                u = shader.uniformTexture(name);
                shader.uniformDefine("GBUFFER_" + slot, name);
            }
            u.set(g);
        }

        resultDef.setSize(cam.getWidth(), cam.getHeight());
        if (resultImage == null) {
            resultImage = new TextureImage(result.acquire(), TextureImage.Access.WriteOnly);
        } else {
            resultImage.setTexture(result.acquire());
        }
        shader.set("ResultImage", resultImage);

        work.setGlobal(cam.getWidth(), cam.getHeight(), 1);
        work.shiftToLocal(2);
        shader.execute(work);

    }

    public void addBufferSlot(String name, String define) {
        shader.uniformTexture(name);
        shader.uniformDefine(define, name);
        bufferSlots.add(name);
    }

}
