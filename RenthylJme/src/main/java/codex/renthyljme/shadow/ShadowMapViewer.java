package codex.renthyljme.shadow;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyljme.sockets.RenderTargetSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

public class ShadowMapViewer extends RasterTask {

    private static ImmediateMatDef matdef;

    private final TransitiveSocket<ShadowMap> shadowMap = new TransitiveSocket<>(this);
    private final RenderTargetSocket<Texture2D> result;
    private final CameraState camera = new CameraState(new Camera(1024, 1024), true);
    private Material material;

    public ShadowMapViewer(ResourceAllocator allocator) {
        addSocket(shadowMap);
        result = addSocket(new RenderTargetSocket<>(this, allocator, TextureDef.texture2D()));
    }

    @Override
    protected void renderTask() {
        if (material == null) {
            material = createMaterial(context.getAssetManager());
        }
        Texture2D map = shadowMap.acquireOrThrow("Shadow map required.").getMap();
        result.getTextureDef().setSize(map.getImage());
        camera.resize(map.getImage().getWidth(), map.getImage().getHeight(), false);
        context.getCamera().pushValue(camera);
        result.getBufferDef().setColorTarget(result.getTexture().acquire());
        FrameBuffer fbo = result.getFrameBuffer().acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();
        material.setTexture("ShadowMap", map);
        context.renderFullscreen(material);
        context.getCamera().pop();
        context.getFrameBuffer().pop();
    }

    public TransitiveSocket<ShadowMap> getShadowMap() {
        return shadowMap;
    }

    public Socket<Texture2D> getResult() {
        return result.getTexture();
    }

    private static Material createMaterial(AssetManager assetManager) {
        if (matdef == null) {
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment)
                    .includeGlslCompat()
                    .uniform("sampler2D", "m_ShadowMap")
                    .varying("vec2", "texCoord")
                    .main()
                    .assign("gl_FragColor.rgb", "vec3(1.0 - texture2D(m_ShadowMap, texCoord).r)")
                    .assign("gl_FragColor.a", "1.0")
                    .end();
            matdef = new ImmediateMatDef(assetManager, "ShadowMapViewer")
                    .addParam(VarType.Texture2D, "ShadowMap");
            matdef.createTechnique()
                    .setVersions(450, 330, 150)
                    .setVertexShader("RenthylJme/MatDefs/Fullscreen/Screen.vert")
                    .setShader(frag)
                    .add();
        }
        return matdef.createMaterial();
    }

}
