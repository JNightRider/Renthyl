/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author codex
 */
public class FrameBufferDef extends AbstractResourceDef<FrameBuffer> {
    
    private int width, height, samples;
    private final ArrayList<Texture> colorTargets = new ArrayList<>();
    private Texture depthTarget;
    private boolean requireColorTargetOrder = true;
    private int highestColorTarget = -1;
    
    @Override
    public FrameBuffer createResource() {
        return new FrameBuffer(width, height, samples);
    }
    @Override
    public FrameBuffer applyDirectResource(Object resource) {
        if (resource instanceof FrameBuffer) {
            FrameBuffer fb = (FrameBuffer)resource;
            if (fb.getWidth() == width && fb.getHeight() == height && fb.getSamples() == samples
                    && (depthTarget == null || fb.getDepthTarget().getTexture() == depthTarget)
                    && fb.getNumColorTargets() > highestColorTarget) {
                main: for (int i = 0; i <= highestColorTarget; i++) {
                    Texture target = colorTargets.get(i);
                    if (target == null) {
                        continue;
                    }
                    if (!requireColorTargetOrder) {
                        FrameBuffer.RenderBuffer attachment = fb.getColorTarget(i);
                        if (target != attachment.getTexture()) {
                            return null;
                        }
                    } else {
                        for (int j = 0; j < fb.getNumColorTargets(); j++) {
                            FrameBuffer.RenderBuffer attachment = fb.getColorTarget(j);
                            if (target == attachment.getTexture()) {
                                continue main;
                            }
                        }
                        return null;
                    }
                }
                return fb;
            }
        }
        return null;
    }
    @Override
    public FrameBuffer applyIndirectResource(Object resource) {
        return null;
    }
    @Override
    public boolean isAllowIndirectResources() {
        return false;
    }

    public void setColorTarget(int i, Texture target) {
        Objects.requireNonNull(target, "Texture target cannot be null.");
        while (colorTargets.size() <= i) {
            colorTargets.add(null);
        }
        highestColorTarget = Math.max(i, highestColorTarget);
        colorTargets.set(i, target);
    }
    public Texture getColorTarget(int i) {
        if (i >= colorTargets.size()) {
            return null;
        }
        return colorTargets.get(i);
    }
    public Texture removeColorTarget(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("Index cannot be less than zero.");
        }
        if (i >= colorTargets.size()) {
            return null;
        }
        Texture t = colorTargets.set(i, null);
        if (i == highestColorTarget) {
            while (--highestColorTarget >= 0 && colorTargets.get(highestColorTarget) == null) {}
        }
        return t;
    }
    
    public void setWidth(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be greater than zero.");
        }
        this.width = width;
    }
    public void setHeight(int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Height must be greater than zero.");
        }
        this.height = height;
    }
    public void setSamples(int samples) {
        if (samples <= 0) {
            throw new IllegalArgumentException("Samples must be greater than zero.");
        }
        this.samples = samples;
    }
    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }
    public void setSize(int width, int height, int samples) {
        setWidth(width);
        setHeight(height);
        setSamples(samples);
    }
    public void setDepthTarget(Texture depthTarget) {
        this.depthTarget = depthTarget;
    }
    public void setRequireColorTargetOrder(boolean requireColorTargetOrder) {
        this.requireColorTargetOrder = requireColorTargetOrder;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public int getSamples() {
        return samples;
    }
    public Texture getDepthTarget() {
        return depthTarget;
    }
    public boolean isRequireColorTargetOrder() {
        return requireColorTargetOrder;
    }
    
}
