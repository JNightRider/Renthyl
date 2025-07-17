/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.definitions;

import codex.boost.export.SavableObject;
import codex.renthyl.definitions.ResourceDef;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;
import java.util.function.Function;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.CustomBuffer;
import org.lwjgl.system.MemoryUtil;

/**
 * Resource definition for Java {@link Buffer Buffers}.
 *
 * @author codex
 * @param <T>
 */
public class BufferDef <T extends Buffer> implements ResourceDef<T> {

    public static final Function<Integer, ByteBuffer> BYTE = BufferUtils::createByteBuffer;
    public static final Function<Integer, IntBuffer> INT = BufferUtils::createIntBuffer;
    public static final Function<Integer, FloatBuffer> FLOAT = BufferUtils::createFloatBuffer;
    public static final Function<Integer, DoubleBuffer> DOUBLE = BufferUtils::createDoubleBuffer;
    public static final Function<Integer, LongBuffer> LONG = BufferUtils::createLongBuffer;
    public static final Function<Integer, ShortBuffer> SHORT = BufferUtils::createShortBuffer;
    
    private final Class<T> type;
    private final Function<Integer, T> factory;
    private int size;
    private int padding = 0;
    private boolean initToZero = false;

    public BufferDef(Class<T> type, Function<Integer, T> factory, int size) {
        this.type = Objects.requireNonNull(type, "Buffer type cannot be null.");
        this.factory = Objects.requireNonNull(factory, "Buffer factory cannot be null.");
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero.");
        }
        this.size = size;
    }
    
    @Override
    public T createResource() {
        T buffer = factory.apply(size + padding);
        buffer.limit(size);
        buffer.position(0);
        return buffer;
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (type.isAssignableFrom(resource.getClass())) {
            T buffer = (T)resource;
            if (buffer.capacity() >= size) {
                return 0f;
            }
        }
        return null;
    }

    @Override
    public T conformResource(Object resource) {
        return prepareBuffer((T)resource);
    }

    @Override
    public void dispose(T buffer) {}
    
    private T prepareBuffer(T buffer) {
        buffer.position(0).limit(size);
        if (initToZero) {
            zeroBuffer(buffer);
        }
        return buffer;
    }
    
    /**
     * Sets the minimum required capacity of buffers.
     * 
     * @param size 
     */
    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero.");
        }
        this.size = size;
    }

    /**
     * Sets the number of elements beyond the minimum size that
     * buffers are created with.
     * <p>
     * default=0
     * 
     * @param padding 
     */
    public void setPadding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Padding cannot be less than zero.");
        }
        this.padding = padding;
    }

    /**
     * Sets buffers to be cleared to zero when acquired.
     * <p>
     * default={@code false}
     * 
     * @param initToZero 
     */
    public void setInitToZero(boolean initToZero) {
        this.initToZero = initToZero;
    }
    
    /**
     * Gets the buffer type this definition handles.
     * 
     * @return 
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Gets the factory responsible for creating new buffers.
     * 
     * @return 
     */
    public Function<Integer, T> getFactory() {
        return factory;
    }

    /**
     * Gets the minimum required size of buffers.
     * 
     * @return 
     */
    public int getSize() {
        return size;
    }

    /**
     * 
     * @return 
     * @see #setPadding(int) 
     */
    public int getPadding() {
        return padding;
    }

    /**
     * 
     * @return 
     * @see #setInitToZero(boolean) 
     */
    public boolean isInitToZero() {
        return initToZero;
    }
    
    public static <T extends Buffer> BufferDef<T> buffer(Class<T> type, int size) {
        if (ByteBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)bytes(size);
        } else if (IntBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)ints(size);
        } else if (FloatBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)floats(size);
        } else if (DoubleBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)doubles(size);
        } else if (LongBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)longs(size);
        } else if (ShortBuffer.class.isAssignableFrom(type)) {
            return (BufferDef<T>)shorts(size);
        }
        throw new UnsupportedOperationException("Unknown buffer type: " + type);
    }
    public static BufferDef<ByteBuffer> bytes(int size) {
        return new BufferDef<>(ByteBuffer.class, BYTE, size);
    }
    public static BufferDef<IntBuffer> ints(int size) {
        return new BufferDef<>(IntBuffer.class, INT, size);
    }
    public static BufferDef<FloatBuffer> floats(int size) {
        return new BufferDef<>(FloatBuffer.class, FLOAT, size);
    }
    public static BufferDef<DoubleBuffer> doubles(int size) {
        return new BufferDef<>(DoubleBuffer.class, DOUBLE, size);
    }
    public static BufferDef<LongBuffer> longs(int size) {
        return new BufferDef<>(LongBuffer.class, LONG, size);
    }
    public static BufferDef<ShortBuffer> shorts(int size) {
        return new BufferDef<>(ShortBuffer.class, SHORT, size);
    }
    
    public static <T extends Buffer> BufferDef<T> buffer(Class<T> type) {
        return buffer(type, 1);
    }
    public static BufferDef<ByteBuffer> bytes() {
        return bytes(1);
    }
    public static BufferDef<IntBuffer> ints() {
        return ints(1);
    }
    public static BufferDef<FloatBuffer> floats() {
        return floats(1);
    }
    public static BufferDef<DoubleBuffer> doubles() {
        return doubles(1);
    }
    public static BufferDef<LongBuffer> longs() {
        return longs(1);
    }
    public static BufferDef<ShortBuffer> shorts() {
        return shorts(1);
    }
    
    public static Savable save(BufferDef def) {
        Class<? extends Buffer> type = def.getType();
        if (ByteBuffer.class.isAssignableFrom(type)) {
            return saveBytes((BufferDef<ByteBuffer>)def);
        } else if (IntBuffer.class.isAssignableFrom(type)) {
            return saveInts((BufferDef<IntBuffer>)def);
        } else if (FloatBuffer.class.isAssignableFrom(type)) {
            return saveFloats((BufferDef<FloatBuffer>)def);
        } else if (DoubleBuffer.class.isAssignableFrom(type)) {
            return saveDoubles((BufferDef<DoubleBuffer>)def);
        } else if (LongBuffer.class.isAssignableFrom(type)) {
            return saveLongs((BufferDef<LongBuffer>)def);
        } else if (ShortBuffer.class.isAssignableFrom(type)) {
            return saveShorts((BufferDef<ShortBuffer>)def);
        }
        throw new UnsupportedOperationException("Unknown buffer type: " + type);
    }
    public static Savable saveBytes(BufferDef<ByteBuffer> def) {
        return new ByteCapsule(def);
    }
    public static Savable saveInts(BufferDef<IntBuffer> def) {
        return new IntCapsule(def);
    }
    public static Savable saveFloats(BufferDef<FloatBuffer> def) {
        return new FloatCapsule(def);
    }
    public static Savable saveDoubles(BufferDef<DoubleBuffer> def) {
        return new DoubleCapsule(def);
    }
    public static Savable saveLongs(BufferDef<LongBuffer> def) {
        return new LongCapsule(def);
    }
    public static Savable saveShorts(BufferDef<ShortBuffer> def) {
        return new ShortCapsule(def);
    }
    
    public static <T extends Buffer> BufferDef<T> load(InputCapsule in, Class<T> type, String name, int defaultSize) throws IOException {
        BufferDefCapsule<T> cap = SavableObject.readSavable(in, name, BufferDefCapsule.class, null);
        if (cap != null) {
            return cap.getBufferDef();
        } else {
            return buffer(type, defaultSize);
        }
    }
    public static BufferDef<ByteBuffer> loadBytes(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, ByteBuffer.class, name, defaultSize);
    }
    public static BufferDef<IntBuffer> loadInts(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, IntBuffer.class, name, defaultSize);
    }
    public static BufferDef<FloatBuffer> loadFloats(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, FloatBuffer.class, name, defaultSize);
    }
    public static BufferDef<DoubleBuffer> loadDoubles(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, DoubleBuffer.class, name, defaultSize);
    }
    public static BufferDef<LongBuffer> loadLongs(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, LongBuffer.class, name, defaultSize);
    }
    public static BufferDef<ShortBuffer> loadShorts(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, ShortBuffer.class, name, defaultSize);
    }

    /**
     * Sets all elements of {@code buf} to zero.
     *
     * @param buf buffer to zero elements of (cannot be null)
     * @throws UnsupportedOperationException if {@code buf} is not a supported buffer type
     * or is not a buffer at all
     */
    public static void zeroBuffer(Object buf) {
        Objects.requireNonNull(buf, "Buffer to zero cannot be null.");
        if (buf instanceof ByteBuffer) {
            BufferUtils.zeroBuffer((ByteBuffer)buf);
        } else if (buf instanceof IntBuffer) {
            BufferUtils.zeroBuffer((IntBuffer)buf);
        } else if (buf instanceof FloatBuffer) {
            BufferUtils.zeroBuffer((FloatBuffer)buf);
        } else if (buf instanceof DoubleBuffer) {
            BufferUtils.zeroBuffer((DoubleBuffer)buf);
        } else if (buf instanceof LongBuffer) {
            BufferUtils.zeroBuffer((LongBuffer)buf);
        } else if (buf instanceof ShortBuffer) {
            BufferUtils.zeroBuffer((ShortBuffer)buf);
        } else if (buf instanceof CustomBuffer) {
            BufferUtils.zeroBuffer((CustomBuffer)buf);
        } else {
            throw new UnsupportedOperationException("Unable to zero buffer of type " + buf.getClass());
        }
    }
    
    public static abstract class BufferDefCapsule <T extends Buffer> implements Savable {

        protected BufferDef<T> def;
        
        public BufferDefCapsule() {}
        public BufferDefCapsule(BufferDef<T> bufferDef) {
            this.def = bufferDef;
        }
        
        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule out = ex.getCapsule(this);
            out.write(def.size, "size", 1);
            out.write(def.padding, "padding", 0);
            out.write(def.initToZero, "initToZero", false);
        }
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule in = im.getCapsule(this);
            def = createDefinition(in.readInt("size", 1));
            def.setPadding(in.readInt("padding", 0));
            def.setInitToZero(in.readBoolean("initToZero", false));
        }
        
        protected abstract BufferDef<T> createDefinition(int size);
        
        public BufferDef<T> getBufferDef() {
            return def;
        }
        
    }
    public static class ByteCapsule extends BufferDefCapsule<ByteBuffer> {

        public ByteCapsule() {}
        public ByteCapsule(BufferDef<ByteBuffer> bufferDef) {
            super(bufferDef);
        }

        @Override
        protected BufferDef<ByteBuffer> createDefinition(int size) {
            return bytes(size);
        }
        
    }
    public static class IntCapsule extends BufferDefCapsule<IntBuffer> {

        public IntCapsule() {}
        public IntCapsule(BufferDef<IntBuffer> bufferDef) {
            super(bufferDef);
        }
        
        @Override
        protected BufferDef<IntBuffer> createDefinition(int size) {
            return ints(size);
        }
        
    }
    public static class FloatCapsule extends BufferDefCapsule<FloatBuffer> {

        public FloatCapsule() {}
        public FloatCapsule(BufferDef<FloatBuffer> bufferDef) {
            super(bufferDef);
        }
        
        @Override
        protected BufferDef<FloatBuffer> createDefinition(int size) {
            return floats(size);
        }
        
    }
    public static class DoubleCapsule extends BufferDefCapsule<DoubleBuffer> {

        public DoubleCapsule() {}
        public DoubleCapsule(BufferDef<DoubleBuffer> bufferDef) {
            super(bufferDef);
        }
        
        @Override
        protected BufferDef<DoubleBuffer> createDefinition(int size) {
            return doubles(size);
        }
        
    }
    public static class LongCapsule extends BufferDefCapsule<LongBuffer> {

        public LongCapsule() {}
        public LongCapsule(BufferDef<LongBuffer> bufferDef) {
            super(bufferDef);
        }
        
        @Override
        protected BufferDef<LongBuffer> createDefinition(int size) {
            return longs(size);
        }
        
    }
    public static class ShortCapsule extends BufferDefCapsule<ShortBuffer> {

        public ShortCapsule() {}
        public ShortCapsule(BufferDef<ShortBuffer> bufferDef) {
            super(bufferDef);
        }

        @Override
        protected BufferDef<ShortBuffer> createDefinition(int size) {
            return shorts(size);
        }
        
    }
    
}
