package codex.renthyljme.gbuffer;

import codex.jmecompute.Stride;
import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.uniforms.GLUniform;
import codex.jmecompute.opengl.uniforms.buffers.FloatArrayUniform;
import codex.jmecompute.opengl.uniforms.buffers.FloatBufferUniform;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyl.tasks.AbstractTask;
import codex.renthylplus.lights.LightBuffer;
import com.jme3.asset.AssetManager;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

import java.util.ArrayList;
import java.util.Collection;

public class GBufferLightingPass extends AbstractTask {

    private final SocketList<? extends Socket<Texture2D>, Texture2D> gbuffers = new SocketList<>(this);
    private final TransitiveSocket<Texture2D> depth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Camera> camera = new TransitiveSocket<>(this);
    private final TransitiveSocket<LightBuffer> lightData = new TransitiveSocket<>(this);
    private final AllocationSocket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D(Image.Format.RGBA32F);
    private final WorkSize work = new WorkSize();
    private final GLComputeShader shader;
    private final Matrix4f viewProjInverse = new Matrix4f();
    private final Collection<String> bufferSlots = new ArrayList<>();
    private TextureImage resultImage;

    public GBufferLightingPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(gbuffers, depth, camera, lightData);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/GBuffers/GBufferPBR.glsl");
        shader.uniformMatrix4("ViewProjectionInverse");
        shader.uniformVector3("CameraPosition");
        shader.uniform(new FloatBufferUniform("LightData", Stride.Vec4));
        shader.uniformTexture("m_DepthGBuffer");
        shader.uniformImage("ResultImage");
        shader.uniformsTexture("g_PrevEnvMap", "g_PrevEnvMap2", "g_PrevEnvMap3");
        shader.uniforms(new FloatArrayUniform("g_ShCoeffs"), new FloatArrayUniform("g_ShCoeffs2"), new FloatArrayUniform("g_ShCoeffs3"));
        shader.uniformsMatrix4("g_LightProbeData", "g_LightProbeData2", "g_LightProbeData3");
    }

    @Override
    protected void renderTask() {

        // camera
        Camera cam = camera.acquireOrThrow("Camera required.");
        shader.set("ViewProjectionInverse", cam.getViewProjectionMatrix().invert());
        shader.set("CameraPosition", cam.getLocation().clone());

        // lights
        LightBuffer lights = lightData.acquireOrThrow("Light data required.");
        shader.set("LightData", lights.getData());
        shader.define("LIGHT_DATA_LENGTH", lights.getData().limit() / 4);
        lights.uploadProbes(shader, 3);

        // gbuffers
        int slot = 1;
        for (Texture2D g : gbuffers.acquire()) {
            String name = "m_GBuffer" + (slot++);
            GLUniform u = shader.getUniform(name);
            if (u == null) {
                u = shader.uniformTexture(name);
            }
            u.set(g);
        }
        shader.set("m_DepthGBuffer", depth.acquireOrThrow("Depth required."));

        // result
        resultDef.setSize(cam.getWidth(), cam.getHeight());
        if (resultImage == null) {
            resultImage = new TextureImage(result.acquire(), TextureImage.Access.WriteOnly);
        } else {
            resultImage.setTexture(result.acquire());
        }
        shader.set("ResultImage", resultImage);

        // execute
        work.clear();
        work.setGlobal(cam.getWidth(), cam.getHeight(), 1);
        work.shiftToLocal(2);
        work.clearZ();
        shader.define("LOCAL_X", work.getLocalX());
        shader.define("LOCAL_Y", work.getLocalY());
        shader.execute(work);

    }

    public SocketList<? extends Socket<Texture2D>, Texture2D> getGbuffers() {
        return gbuffers;
    }

    public PointerSocket<Texture2D> getDepth() {
        return depth;
    }

    public PointerSocket<Camera> getCamera() {
        return camera;
    }

    public PointerSocket<LightBuffer> getLightData() {
        return lightData;
    }

    public Socket<Texture2D> getResult() {
        return result;
    }

    public TextureDef<Texture2D> getResultDef() {
        return resultDef;
    }

}
