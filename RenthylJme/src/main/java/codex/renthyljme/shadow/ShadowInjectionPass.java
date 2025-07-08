package codex.renthyljme.shadow;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.filter.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;

public class ShadowInjectionPass extends AbstractFilterTask {

    private final TransitiveSocket<Texture2D> contribution = new TransitiveSocket<>(this);
    private final ArgumentSocket<Integer> numLights = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> intensity = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<ColorRGBA> shadowColor = new ArgumentSocket<>(this, ColorRGBA.Black.clone());

    public ShadowInjectionPass(AssetManager assetManager, ResourceAllocator allocator) {
        super(allocator, new Material(assetManager, "RenthylJme/MatDefs/Shadows/ShadowInject.j3md"), false);
        addSockets(contribution, numLights, intensity, shadowColor);
    }

    @Override
    protected void configureMaterial(Material material) {
        contribution.acquireToMaterial(material, "LightContribution");
        numLights.acquireToMaterial(material, "NumLights");
        intensity.acquireToMaterial(material, "Intensity");
        shadowColor.acquireToMaterial(material, "ShadowColor");
    }

    public TransitiveSocket<Texture2D> getContribution() {
        return contribution;
    }

    public ArgumentSocket<Integer> getNumLights() {
        return numLights;
    }

    public ArgumentSocket<Float> getIntensity() {
        return intensity;
    }

    public ArgumentSocket<ColorRGBA> getShadowColor() {
        return shadowColor;
    }

}
