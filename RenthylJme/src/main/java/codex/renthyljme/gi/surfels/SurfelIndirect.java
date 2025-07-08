package codex.renthyljme.gi.surfels;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.NativeBuffer;
import codex.jmecompute.opengl.uniforms.Matrix4Uniform;
import codex.jmecompute.opengl.uniforms.bufferobjects.BufferShaderStorage;
import codex.jmecompute.opengl.uniforms.textures.ImageUniform;
import codex.jmecompute.opengl.uniforms.textures.TextureUniform;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.OptionalSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.RasterTask;
import codex.renthyljme.filter.PostProcessFilter;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

import java.nio.FloatBuffer;

public class SurfelIndirect extends RasterTask implements PostProcessFilter {

    private final TransitiveSocket<Texture2D> sceneColor = new OptionalSocket<>(this, false);
    private final TransitiveSocket<Texture2D> sceneDepth = new TransitiveSocket<>(this);
    private final TransitiveSocket<NativeBuffer<FloatBuffer>> surfels = new TransitiveSocket<>(this);
    private final TransitiveSocket<Camera> camera = new TransitiveSocket<>(this);
    private final AllocationSocket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D();
    private final GLComputeShader shader;
    private final WorkSize work = new WorkSize();
    private TextureImage resultImage;

    public SurfelIndirect(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(sceneColor, sceneDepth, surfels, camera);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/Surfels/indirect.glsl");
        shader.uniform(new TextureUniform("DepthMap"));
        shader.uniform(new BufferShaderStorage("Surfels", BufferObject.AccessHint.Dynamic, BufferObject.NatureHint.Copy));
        shader.uniform(new Matrix4Uniform("ViewProjectionMatrix"));
        shader.uniform(new ImageUniform("IndirectMap"));
    }

    @Override
    protected void renderTask() {
        NativeBuffer<FloatBuffer> surfelBuf = surfels.acquireOrThrow("Surfel buffer required.");
        shader.set("DepthMap", sceneDepth.acquireOrThrow("Scene depth required."));
        shader.set("Surfels", surfelBuf);
        shader.set("ViewProjectionMatrix", camera.acquireOrThrow("Camera required.").getViewProjectionMatrix());
        if (resultImage == null) {
            resultImage = new TextureImage(result.acquire(), TextureImage.Access.WriteOnly);
        } else {
            resultImage.setTexture(result.acquire());
        }
        shader.set("IndirectMap", resultImage);
        //work.clear().setGlobal(resultDef.getWidth(), resultDef.getHeight(), 1).setLocal();
        shader.execute(work);
    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return sceneColor;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return sceneDepth;
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return result;
    }

}
