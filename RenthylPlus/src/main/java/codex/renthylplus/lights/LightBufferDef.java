package codex.renthylplus.lights;

import codex.renthyl.definitions.ResourceDef;

import java.nio.FloatBuffer;

public class LightBufferDef implements ResourceDef<LightBuffer> {

    private int size;
    private int padding = 0;

    public LightBufferDef() {
        this(3);
    }
    public LightBufferDef(int size) {
        this.size = size;
    }

    @Override
    public LightBuffer createResource() {
        return new LightBuffer(size + padding);
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof LightBuffer) {
            LightBuffer lb = (LightBuffer)resource;
            if (lb.capacity() >= size) {
                return 0f;
            }
        } else if (resource instanceof FloatBuffer) {
            FloatBuffer fb = (FloatBuffer)resource;
            if (fb.capacity() >= size * LightBuffer.FLOATS_PER_LIGHT) {
                return 1f; // prefer LightBuffers over raw FloatBuffers
            }
        }
        return null;
    }

    @Override
    public LightBuffer conformResource(Object resource) {
        if (resource instanceof LightBuffer) {
            return (LightBuffer)resource;
        } else {
            return new LightBuffer((FloatBuffer)resource);
        }
    }

    @Override
    public void dispose(LightBuffer object) {}

    public void setSize(int size) {
        this.size = size;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getSize() {
        return size;
    }

    public int getPadding() {
        return padding;
    }

}
