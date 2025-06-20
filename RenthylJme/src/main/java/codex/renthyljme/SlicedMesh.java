package codex.renthyljme;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import org.lwjgl.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class SlicedMesh extends Mesh {

    public SlicedMesh(Mesh base, int slices) {
        this(base, slices, VertexBuffer.Type.TexCoord3);
    }

    public SlicedMesh(Mesh base, int slices, VertexBuffer.Type layerBufferType) {
        assert slices > 0 : "Mesh must have at least one slice.";
        int vertCount = base.getBuffer(VertexBuffer.Type.Position).getData().limit() / 3;
        for (VertexBuffer vb : base.getBufferList()) {
            Buffer data = slice(vb, slices, vertCount);
            data.flip();
            setBuffer(vb.getBufferType(), vb.getNumComponents(), vb.getFormat(), data);
        }
        FloatBuffer layers = BufferUtils.createFloatBuffer(vertCount * slices);
        float sliceHeight = 1f / slices;
        for (int i = 0; i < slices; i++) {
            float l = sliceHeight * i;
            for (int j = 0; j < vertCount; j++) {
                layers.put(l);
            }
        }
        layers.flip();
        setBuffer(layerBufferType, 1, VertexBuffer.Format.Float, layers);
        setStatic();
        updateCounts();
        updateBound();
    }

    private Buffer slice(VertexBuffer base, int slices, int vertCount) {
        if (base.getBufferType() == VertexBuffer.Type.Index) {
            return sliceIndices(base, slices, vertCount);
        }
        switch (base.getFormat()) {
            case Float: return sliceFloat(base, slices);
            case Int: return sliceInt(base, slices);
            case Byte: return sliceByte(base, slices);
            default: throw new UnsupportedOperationException("Unable to slice " + base.getFormat() + " buffer.");
        }
    }

    private IntBuffer sliceIndices(VertexBuffer base, int slices, int vertCount) {
        IntBuffer target = BufferUtils.createIntBuffer(base.getData().limit() * slices);
        IntBuffer source = (IntBuffer)base.getData();
        // render higher slices first
        for (int i = slices - 1; i >= 0; i--) {
            for (int j = 0; j < source.limit(); j++) {
                target.put(source.get(j) + i * vertCount);
            }
        }
        return target;
    }

    private FloatBuffer sliceFloat(VertexBuffer base, int slices) {
        FloatBuffer target = BufferUtils.createFloatBuffer(base.getData().limit() * slices);
        FloatBuffer source = (FloatBuffer)base.getData();
        source.position(0);
        target.position(0);
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < source.limit(); j++) {
                target.put(source.get(j));
            }
        }
        source.position(0);
        return target;
    }

    private IntBuffer sliceInt(VertexBuffer base, int slices) {
        IntBuffer target = BufferUtils.createIntBuffer(base.getData().limit() * slices);
        IntBuffer source = (IntBuffer) base.getData();
        source.position(0);
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < source.limit(); j++) {
                target.put(source.get(j));
            }
        }
        source.position(0);
        return target;
    }

    private ByteBuffer sliceByte(VertexBuffer base, int slices) {
        ByteBuffer target = BufferUtils.createByteBuffer(base.getData().limit() * slices);
        ByteBuffer source = (ByteBuffer) base.getData();
        source.position(0);
        for (int i = 0; i < slices; i++) {
            for (int j = 0; j < source.limit(); j++) {
                target.put(source.get(j));
            }
        }
        source.position(0);
        return target;
    }

    public static Mesh slice(Mesh mesh, int slices) {
        FloatBuffer data = BufferUtils.createFloatBuffer(slices);
        for (int i = 0; i < data.capacity(); i++) {
            data.put(i, (float)i / slices);
        }
        data.clear();
        VertexBuffer instances = new VertexBuffer(VertexBuffer.Type.TexCoord3);
        instances.setupData(VertexBuffer.Usage.Static, 1, VertexBuffer.Format.Float, data);
        mesh.setBuffer(instances);
        instances.setInstanced(true);
        mesh.updateCounts();
        mesh.updateBound();
        return mesh;
    }

}
