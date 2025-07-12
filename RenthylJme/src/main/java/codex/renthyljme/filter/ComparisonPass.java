package codex.renthyljme.filter;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.utils.MaterialUtils;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.shader.Shader;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture2D;

public class ComparisonPass extends AbstractFilterTask {

    private static ImmediateMatDef matdef;

    private final TransitiveSocket<Texture2D> compare = new TransitiveSocket<>(this);
    private final ArgumentSocket<Float> divide = new ArgumentSocket<>(this, 0.5f);

    public ComparisonPass(ResourceAllocator allocator) {
        super(allocator, false);
        addSockets(compare, divide);
    }

    @Override
    protected Material createMaterial(AssetManager assetManager) {
        if (matdef == null) {
            ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment)
                    .includeGlslCompat()
                    .uniform("sampler2D", "m_Texture").uniform("sampler2D", "m_Texture2").uniform("float", "m_Divide")
                    .varying("vec2", "texCoord")
                    .main()
                        ._if("texCoord.x < m_Divide").assign("gl_FragColor", "texture2D(m_Texture, texCoord)")
                        ._else().assign("gl_FragColor", "texture2D(m_Texture2, texCoord)").end()
                    .end();
            matdef = new ImmediateMatDef(assetManager, "TextureComparator")
                    .addParam(VarType.Texture2D, "Texture")
                    .addParam(VarType.Texture2D, "Texture2")
                    .addParam(VarType.Float, "Divide");
            matdef.createTechnique()
                    .setVersions(450, 310, 210)
                    .setVertexShader("RenthylJme/MatDefs/Fullscreen/Screen.vert")
                    .setShader(frag)
                    .add();
        }
        return matdef.createMaterial();
    }

    @Override
    protected void configureMaterial(Material material) {
        MaterialUtils.acquireToMaterial(material, "Texture2", compare);
        MaterialUtils.acquireToMaterial(material, "Divide", divide);
    }

}
