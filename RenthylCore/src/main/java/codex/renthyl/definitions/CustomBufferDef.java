/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions;

import codex.boost.export.SavableObject;
import codex.renthyl.resources.ResourceException;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import org.lwjgl.BufferUtils;
import org.lwjgl.CLongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.CustomBuffer;

/**
 *
 * @author codex
 * @param <T>
 */
public class CustomBufferDef <T extends CustomBuffer> extends AbstractResourceDef<T> {
    
    public static final Function<Integer, PointerBuffer> POINTER = n -> BufferUtils.createPointerBuffer(n);
    public static final Function<Integer, CLongBuffer> CLONG = n -> BufferUtils.createCLongBuffer(n);
    
    private final Class<T> type;
    private final Function<Integer, T> factory;
    private int size;
    private int padding = 0;
    private boolean initToZero = false;

    public CustomBufferDef(Class<T> type, Function<Integer, T> factory) {
        this(type, factory, 1);
    }
    public CustomBufferDef(Class<T> type, Function<Integer, T> factory, int size) {
        this.type = type;
        this.factory = factory;
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
            if (buffer.capacity() <= size) {
                return 0f;
            }
        }
        return null;
    }
    @Override
    public T conformResource(Object resource) throws ResourceException {
        return prepareBuffer((T)resource);
    }
    
    private T prepareBuffer(T buffer) {
        buffer.limit(size);
        buffer.position(0);
        if (initToZero) {
            BufferUtils.zeroBuffer(buffer);
        }
        return buffer;
    }

    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero.");
        }
        this.size = size;
    }
    public void setPadding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Buffer padding cannot be less than zero.");
        }
        this.padding = padding;
    }
    public void setInitToZero(boolean initToZero) {
        this.initToZero = initToZero;
    }

    public Class<T> getType() {
        return type;
    }
    public Function<Integer, T> getFactory() {
        return factory;
    }
    public int getSize() {
        return size;
    }
    public int getPadding() {
        return padding;
    }
    public boolean isInitToZero() {
        return initToZero;
    }
    
    public static <T extends CustomBuffer> CustomBufferDef<T> buffer(Class<T> type, int size) {
        if (PointerBuffer.class.isAssignableFrom(type)) {
            return (CustomBufferDef<T>)pointers(size);
        } else if (CLongBuffer.class.isAssignableFrom(type)) {
            return (CustomBufferDef<T>)cLongs(size);
        }
        throw new UnsupportedOperationException("Unknown buffer type: " + type);
    }
    public static <T extends CustomBuffer> CustomBufferDef<T> buffer(Class<T> type) {
        return buffer(type, 1);
    }
    public static CustomBufferDef<PointerBuffer> pointers(int size) {
        return new CustomBufferDef<>(PointerBuffer.class, POINTER, size);
    }
    public static CustomBufferDef<PointerBuffer> pointers() {
        return pointers(1);
    }
    public static CustomBufferDef<CLongBuffer> cLongs(int size) {
        return new CustomBufferDef<>(CLongBuffer.class, CLONG, size);
    }
    public static CustomBufferDef<CLongBuffer> cLongs() {
        return cLongs(1);
    }
    
    public static <T extends CustomBuffer> Savable save(CustomBufferDef<T> def) {
        Objects.requireNonNull(def, "Definition to save cannot be null.");
        Class<T> type = def.getType();
        if (PointerBuffer.class.isAssignableFrom(type)) {
            return savePointers((CustomBufferDef<PointerBuffer>)def);
        } else if (CLongBuffer.class.isAssignableFrom(type)) {
            return saveCLongs((CustomBufferDef<CLongBuffer>)def);
        }
        throw new UnsupportedOperationException("Unknown buffer type: " + type);
    }
    public static Savable savePointers(CustomBufferDef<PointerBuffer> def) {
        return new PointerCapsule(def);
    }
    public static Savable saveCLongs(CustomBufferDef<CLongBuffer> def) {
        return new CLongCapsule(def);
    }
    
    public static <T extends CustomBuffer> CustomBufferDef<T> load(InputCapsule in, Class<T> type, String name, int defaultSize) throws IOException {
        CustomBufferDefCapsule<T> cap = SavableObject.readSavable(in, name, CustomBufferDefCapsule.class, null);
        if (cap != null) {
            return cap.getCustomBufferDef();
        } else {
            return buffer(type, defaultSize);
        }
    }
    public static CustomBufferDef<PointerBuffer> loadPointers(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, PointerBuffer.class, name, defaultSize);
    }
    public static CustomBufferDef<CLongBuffer> loadCLongs(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, CLongBuffer.class, name, defaultSize);
    }
    
    public static abstract class CustomBufferDefCapsule <T extends CustomBuffer> implements Savable {
        
        protected CustomBufferDef<T> def;
        
        public CustomBufferDefCapsule() {}
        public CustomBufferDefCapsule(CustomBufferDef<T> def) {
            this.def = def;
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
        
        protected abstract CustomBufferDef<T> createDefinition(int size);
        
        public CustomBufferDef<T> getCustomBufferDef() {
            return def;
        }
        
    }
    public static class PointerCapsule extends CustomBufferDefCapsule<PointerBuffer> {

        public PointerCapsule() {}
        public PointerCapsule(CustomBufferDef<PointerBuffer> def) {
            super(def);
        }
        
        @Override
        protected CustomBufferDef<PointerBuffer> createDefinition(int size) {
            return pointers(size);
        }
        
    }
    public static class CLongCapsule extends CustomBufferDefCapsule<CLongBuffer> {

        public CLongCapsule() {}
        public CLongCapsule(CustomBufferDef<CLongBuffer> def) {
            super(def);
        }
        
        @Override
        protected CustomBufferDef<CLongBuffer> createDefinition(int size) {
            return cLongs(size);
        }
        
    }
    
}
