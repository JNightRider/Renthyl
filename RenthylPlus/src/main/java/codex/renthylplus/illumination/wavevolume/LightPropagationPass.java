package codex.renthylplus.illumination.wavevolume;

import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.TemporalSocket;
import codex.renthyl.tasks.AbstractTask;
import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture3D;

public class LightPropagationPass extends AbstractTask {

    private final TemporalSocket<Texture3D> volume;
    private final TextureDef<Texture3D> volumeDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final GLComputeShader shader;

    public LightPropagationPass(AssetManager assetManager, ResourceAllocator allocator) {
        volume = addSocket(new TemporalSocket<>(this, allocator, volumeDef, 1));
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/WaveVolume/lightPropagation.glsl");
    }

    @Override
    protected void renderTask() {

    }

}
