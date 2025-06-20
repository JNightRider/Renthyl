package codex.renthylplus.illumination;

import com.jme3.math.Matrix4f;
import com.jme3.texture.TextureArray;

public class CaptureMap {

    private final TextureArray texture;
    private final Matrix4f viewProjectionMatrix = new Matrix4f();

    public CaptureMap(TextureArray texture, Matrix4f viewProjectionMatrix) {
        this.texture = texture;
        this.viewProjectionMatrix.set(viewProjectionMatrix);
    }

    public void setViewProjectionMatrix(Matrix4f viewProjectionMatrix) {
        this.viewProjectionMatrix.set(viewProjectionMatrix);
    }

    public TextureArray getTexture() {
        return texture;
    }

    public Matrix4f getViewProjectionMatrix() {
        return viewProjectionMatrix;
    }

}
