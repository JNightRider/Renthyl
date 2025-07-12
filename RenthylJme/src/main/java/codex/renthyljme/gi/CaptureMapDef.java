package codex.renthyljme.gi;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyljme.definitions.TextureDef;
import com.jme3.math.Matrix4f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureArray;

public class CaptureMapDef implements ResourceDef<CaptureMap> {

    private final TextureDef<TextureArray> textureDef;
    private final Matrix4f viewProjection = new Matrix4f();

    public CaptureMapDef() {
        this(TextureDef.textureArray());
    }
    public CaptureMapDef(TextureDef<TextureArray> textureDef) {
        this.textureDef = textureDef;
    }

    @Override
    public CaptureMap createResource() {
        return new CaptureMap(textureDef.createResource(), viewProjection);
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof CaptureMap) {
            return textureDef.evaluateResource(((CaptureMap)resource).getTexture());
        }
        if (resource instanceof Texture || resource instanceof Image) {
            Float raw = textureDef.evaluateResource(resource);
            return raw != null ? raw + 1f : null; // prefer CaptureMaps over other usable resources
        }
        return null;
    }

    @Override
    public CaptureMap conformResource(Object resource) {
        if (resource instanceof CaptureMap) {
            CaptureMap res = (CaptureMap)resource;
            res.setViewProjectionMatrix(viewProjection);
            return res;
        } else if (resource instanceof TextureArray) {
            return new CaptureMap((TextureArray) resource, viewProjection);
        } else if (resource instanceof Texture) {
            TextureArray t = new TextureArray();
            t.setImage(((Texture)resource).getImage());
            return new CaptureMap(t, viewProjection);
        } else if (resource instanceof Image) {
            TextureArray t = new TextureArray();
            t.setImage((Image)resource);
            return new CaptureMap(t, viewProjection);
        }
        throw new IllegalArgumentException("Resource is not an accepted type.");
    }

    @Override
    public void dispose(CaptureMap object) {}

    public void setViewProjection(Matrix4f viewProjection) {
        this.viewProjection.set(viewProjection);
    }

    public TextureDef<TextureArray> getTextureDef() {
        return textureDef;
    }

    public Matrix4f getViewProjection() {
        return viewProjection;
    }

}
