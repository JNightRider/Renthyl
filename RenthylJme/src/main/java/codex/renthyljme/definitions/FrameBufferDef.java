package codex.renthyljme.definitions;

import codex.renthyl.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

/**
 * Resource definition for FrameBuffers. Only approves FrameBuffers which have certain color and depth targets
 * in a certain order with no extra targets. Each target is expected to have the same demensions and number
 * of samples.
 *
 * @author codex
 */
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
        if (!(resource instanceof FrameBuffer fbo)) {
            return null;
        }
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
    public FrameBuffer conformResource(Object resource) {
        return (FrameBuffer)resource;
    }

    @Override
    public void dispose(FrameBuffer object) {
        object.dispose();
    }

    /**
     * Sets the array of color targets. FrameBuffers must contain the color targets
     * in this same order with no extraneous targets to be accepted. All targets are
     * assumed to have the same demensions and number of samples.
     *
     * <p>If the array is set to null or an array of length 0, FrameBuffers must contain
     * no color targets.</p>
     *
     * @param colorTargets color target array (may be null or of length 0, otherwise
     *                     elements must be non-null)
     */
    public void setColorTargets(Texture... colorTargets) {
        this.colorTargets = colorTargets;
        if (this.colorTargets != null && this.colorTargets.length > 0) {
            updateProperties(this.colorTargets[0].getImage());
        }
    }

    /**
     * Sets the color target array to be an array of length one containing {@code colorTarget}.
     *
     * @param colorTarget single color target (must be non-null)
     */
    public void setColorTarget(Texture colorTarget) {
        assert colorTarget != null;
        if (colorTargets == null || colorTargets.length != 1) {
            colorTargets = new Texture[1];
        }
        colorTargets[0] = colorTarget;
        updateProperties(colorTarget.getImage());
    }

    /**
     * Sets the color target array to null.
     */
    public void clearColorTargets() {
        colorTargets = null;
    }

    /**
     * Sets the depth target.
     *
     * @param depthTarget depth target (must be non-null)
     */
    public void setDepthTarget(Texture depthTarget) {
        this.depthTarget = depthTarget;
        if (this.depthTarget != null) {
            updateProperties(this.depthTarget.getImage());
        }
    }

    private void updateProperties(Image img) {
        width = img.getWidth();
        height = img.getHeight();
        samples = img.getMultiSamples();
    }

}
