package codex.renthyl.util;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

import java.util.Objects;

public class FBuffer extends FrameBuffer {

    public FBuffer(int width, int height, int samples) {
        super(width, height, samples);
    }

    protected FBuffer(FrameBuffer src) {
        super(src);
    }

    public void setColorTargets(Texture... targets) {
        for (int i = 0; i < targets.length; i++) {
            Texture t = Objects.requireNonNull(targets[i], "Target color texture cannot be null.");
            if (i >= getNumColorTargets() && getColorTarget(i).getTexture() != t) {
                replaceColorTarget(i, FrameBuffer.FrameBufferTarget.newTarget(t));
                setUpdateNeeded();
            }
        }
        while (getNumColorTargets() > targets.length) {
            removeColorTarget(getNumColorTargets() - 1);
            setUpdateNeeded();
        }
    }

    public void setDepthTarget(Texture target) {
        Objects.requireNonNull(target, "Target depth texture cannot be null.");
        if (getDepthTarget() == null || getDepthTarget().getTexture() != target) {
            setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(target));
            setUpdateNeeded();
        }
    }

}
