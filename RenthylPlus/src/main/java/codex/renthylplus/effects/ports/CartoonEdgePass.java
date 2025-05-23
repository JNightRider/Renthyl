package codex.renthylplus.effects.ports;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthylplus.effects.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

public class CartoonEdgePass extends AbstractFilterTask {

    private final TransitiveSocket<Texture> normals = new TransitiveSocket<>(this);
    private final ArgumentSocket<Float> edgeWidth = new ArgumentSocket<>(this, 1.0f);
    private final ArgumentSocket<Float> edgeIntensity = new ArgumentSocket<>(this, 1.0f);
    private final ArgumentSocket<Float> normalThreshold = new ArgumentSocket<>(this, 0.5f);
    private final ArgumentSocket<Float> depthThreshold = new ArgumentSocket<>(this, 0.1f);
    private final ArgumentSocket<Float> normalSensitivity = new ArgumentSocket<>(this, 1.0f);
    private final ArgumentSocket<Float> depthSensitivity = new ArgumentSocket<>(this, 10.0f);
    private final ArgumentSocket<ColorRGBA> edgeColor = new ArgumentSocket<>(this, ColorRGBA.Black.clone());

    public CartoonEdgePass(AssetManager assetManager, ResourceAllocator allocator) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/CartoonEdge.j3md"), true);
        addSockets(normals, edgeWidth, edgeIntensity, normalThreshold, depthThreshold, normalSensitivity, depthSensitivity, edgeColor);
    }

    @Override
    protected void configureMaterial(Material material) {
        normals.acquireToMaterial(material, "NormalsTexture");
        edgeWidth.acquireToMaterial(material, "EdgeWidth");
        edgeIntensity.acquireToMaterial(material, "EdgeIntensity");
        normalThreshold.acquireToMaterial(material, "NormalThreshold");
        depthThreshold.acquireToMaterial(material, "DepthThreshold");
        normalSensitivity.acquireToMaterial(material, "NormalSensitivity");
        depthSensitivity.acquireToMaterial(material, "DepthSensitivity");
        edgeColor.acquireToMaterial(material, "EdgeColor");
    }

    public PointerSocket<Texture> getNormals() {
        return normals;
    }

    public ArgumentSocket<Float> getEdgeWidth() {
        return edgeWidth;
    }

    public ArgumentSocket<Float> getEdgeIntensity() {
        return edgeIntensity;
    }

    public ArgumentSocket<Float> getNormalThreshold() {
        return normalThreshold;
    }

    public ArgumentSocket<Float> getDepthThreshold() {
        return depthThreshold;
    }

    public ArgumentSocket<Float> getNormalSensitivity() {
        return normalSensitivity;
    }

    public ArgumentSocket<Float> getDepthSensitivity() {
        return depthSensitivity;
    }

    public ArgumentSocket<ColorRGBA> getEdgeColor() {
        return edgeColor;
    }

}
