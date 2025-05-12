package codex.renthyl.definitions;

import codex.renthyl.resources.ResourceException;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

public class FrameBufferDef implements ResourceDef<FrameBuffer> {

    private int width, height, samples;
    private Texture[] colorTargets;
    private Texture depthTarget;

    @Override
    public FrameBuffer createResource() {
        FrameBuffer fbo = new FrameBuffer(width, height, samples);
        if (colorTargets != null) for (Texture t : colorTargets) {
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(t));
        }
        if (depthTarget != null) {
            fbo.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depthTarget));
        }
        return fbo;
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (!(resource instanceof FrameBuffer)) {
            return null;
        }
        FrameBuffer fbo = (FrameBuffer)resource;
        if (fbo.getWidth() != width || fbo.getHeight() != height || fbo.getSamples() != samples) {
            return null;
        }
        Texture depth = fbo.getDepthTarget() != null ? fbo.getDepthTarget().getTexture() : null;
        if (depthTarget != depth) {
            return null;
        }
        if ((colorTargets == null || colorTargets.length == 0) && fbo.getNumColorTargets() == 0) {
            return 0f;
        }
        if (colorTargets == null || colorTargets.length != fbo.getNumColorTargets()) {
            return null;
        }
        for (int i = 0; i < colorTargets.length; i++) {
            if (i >= fbo.getNumColorTargets()) {
                return null;
            }
            FrameBuffer.RenderBuffer c = fbo.getColorTarget(i);
            if (c == null || colorTargets[i] != c.getTexture()) {
                return null;
            }
        }
        return 0f;
    }

    @Override
    public FrameBuffer conformResource(Object resource) throws ResourceException {
        return (FrameBuffer)resource;
    }

    @Override
    public void dispose(FrameBuffer object) {
        object.dispose();
    }

    public void setSize(int width, int height, int samples) {
        this.width = width;
        this.height = height;
        this.samples = samples;
    }

    public void setColorTargets(Texture... colorTargets) {
        this.colorTargets = colorTargets;
    }

    public void setDepthTarget(Texture depthTarget) {
        this.depthTarget = depthTarget;
    }

}
