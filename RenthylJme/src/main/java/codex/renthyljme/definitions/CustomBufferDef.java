/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.definitions;

import codex.boost.export.SavableObject;
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
 * Resource definition for LWJGL {@link CustomBuffer CustomBuffers}.
 *
 * <p>The definition specifies a minimum size which buffer {@link CustomBuffer#capacity() capacity} must be
 * greater than or equal to. The buffer {@link CustomBuffer#limit(int) limit} is set to the minimum size when
 * configured. The buffer contents do not matter for evaluation, but can optionally be set to all zeros on
 * configuration. Buffers are created with a capacity equal to the minimum size plus {@link #setPadding(int)
 * padding}.</p>
 *
 * <p>Definitions can be saved and loaded using a {@link CustomBufferDefCapsule}. Static helper methods and
 * classes for creating, saving, and loading {@link PointerBuffer PointerBuffers} and {@link CLongBuffer
 * CLongBuffers} are provided.</p>
 *
 * @author codex
 * @param <T>
 */
public class CustomBufferDef <T extends CustomBuffer> implements ResourceDef<T> {
    
    public static final Function<Integer, PointerBuffer> POINTER = BufferUtils::createPointerBuffer;
    public static final Function<Integer, CLongBuffer> CLONG = BufferUtils::createCLongBuffer;
    
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
    public T conformResource(Object resource) {
        return prepareBuffer((T)resource);
    }

    @Override
    public void dispose(T buffer) {}
    
    private T prepareBuffer(T buffer) {
        buffer.limit(size);
        buffer.position(0);
        if (initToZero) {
            BufferUtils.zeroBuffer(buffer);
        }
        return buffer;
    }

    /**
     * Sets the minimum buffer capacity.
     *
     * @param size minimum size (cannot be negative)
     */
    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero.");
        }
        this.size = size;
    }

    /**
     * Sets the extra space beyond the {@link #setSize(int) minimum size} that buffers
     * are created with.
     *
     * @param padding buffer padding (must be positive)
     */
    public void setPadding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Buffer padding cannot be less than zero.");
        }
        this.padding = padding;
    }

    /**
     * Sets the definition to configure all buffer elements to zero.
     *
     * @param initToZero
     */
    public void setInitToZero(boolean initToZero) {
        this.initToZero = initToZero;
    }

    /**
     * Gets the buffer type.
     *
     * @return
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Gets the factory that creates new buffers.
     *
     * @return
     */
    public Function<Integer, T> getFactory() {
        return factory;
    }

    /**
     * Gets the minimum buffer capacity.
     *
     * @return
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the buffer padding.
     *
     * @return
     */
    public int getPadding() {
        return padding;
    }

    /**
     * Returns true if the definition will configure buffer elements to zero.
     *
     * @return
     */
    public boolean isInitToZero() {
        return initToZero;
    }

    /**
     * Creates a CustomBufferDef for CustomBuffers of type {@code type} and minimum size {@code size}.
     *
     * @param type type of CustomBuffer
     * @param size initial minimum buffer capacity
     * @return buffer definition
     * @param <T>
     * @throws UnsupportedOperationException if {@code type} does not represent a supported type
     */
    public static <T extends CustomBuffer> CustomBufferDef<T> buffer(Class<T> type, int size) {
        if (PointerBuffer.class.isAssignableFrom(type)) {
            return (CustomBufferDef<T>)pointers(size);
        } else if (CLongBuffer.class.isAssignableFrom(type)) {
            return (CustomBufferDef<T>)cLongs(size);
        }
        throw new UnsupportedOperationException("Unknown buffer type: " + type);
    }

    /**
     * Creates a CustomBufferDef for CustomBuffers of type {@code type} and a minimum size of 1.
     *
     * @param type type of CustomBuffer
     * @return buffer definition
     * @param <T>
     * @throws UnsupportedOperationException if {@code type} does not represent a supported type
     */
    public static <T extends CustomBuffer> CustomBufferDef<T> buffer(Class<T> type) {
        return buffer(type, 1);
    }

    /**
     * Creates a CustomBufferDef for {@link PointerBuffer PointerBuffers} with a minimum size of {@code size}.
     *
     * @param size minimum buffer capacity
     * @return
     */
    public static CustomBufferDef<PointerBuffer> pointers(int size) {
        return new CustomBufferDef<>(PointerBuffer.class, POINTER, size);
    }

    /**
     * Creates a CustomBufferDef for {@link PointerBuffer PointerBuffers} with a minimum size of 1.
     *
     * @return
     */
    public static CustomBufferDef<PointerBuffer> pointers() {
        return pointers(1);
    }

    /**
     * Creates a CustomBufferDef for {@link CLongBuffer CLongBuffers} with a minimum size of {@code size}.
     *
     * @param size minimum buffer capacity
     * @return
     */
    public static CustomBufferDef<CLongBuffer> cLongs(int size) {
        return new CustomBufferDef<>(CLongBuffer.class, CLONG, size);
    }

    /**
     * Creates a CustomBufferDef for {@link CLongBuffer CLongBuffers} with a minimum size of 1.
     *
     * @return
     */
    public static CustomBufferDef<CLongBuffer> cLongs() {
        return cLongs(1);
    }

    /**
     * Creates a {@link Savable} for {@code def}.
     *
     * @param def definition to save
     * @return savable representing {@code def}
     * @param <T>
     * @throws UnsupportedOperationException if {@code def} is not a supported type
     */
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

    /**
     * Creates a {@link Savable} for {@code def} as a {@link PointerBuffer} definition.
     *
     * @param def definition to save
     * @return savable representing {@code def}
     */
    public static Savable savePointers(CustomBufferDef<PointerBuffer> def) {
        return new PointerCapsule(def);
    }

    /**
     * Creates a {@link Savable} for {@code def} as a {@link CLongBuffer} definition.
     *
     * @param def definition to save
     * @return savable representing {@code def}
     */
    public static Savable saveCLongs(CustomBufferDef<CLongBuffer> def) {
        return new CLongCapsule(def);
    }

    /**
     * Loads a CustomBuffer definition from an {@link InputCapsule}.
     *
     * <p>Creates a new definition with a minimum size of {@code defaultSize} from scratch if the
     * input capsule returns null when loading.</p>
     *
     * @param in input capsule to load from
     * @param type buffer type of the definition
     * @param name name of the definition in the capsule
     * @param defaultSize minimum size used if the capsule contains no non-null element at {@code name}
     * @return loaded CustomBuffer definition, or a completely new definition if no definition can be loaded
     * @param <T>
     * @throws IOException
     */
    public static <T extends CustomBuffer> CustomBufferDef<T> load(InputCapsule in, Class<T> type, String name, int defaultSize) throws IOException {
        CustomBufferDefCapsule<T> cap = SavableObject.readSavable(in, name, CustomBufferDefCapsule.class, null);
        if (cap != null) {
            return cap.getCustomBufferDef();
        } else {
            return buffer(type, defaultSize);
        }
    }

    /**
     * {@link #load(InputCapsule, Class, String, int) Loads} a PointerBuffer definition from an {@link InputCapsule}.
     *
     * @param in input capsule to load from
     * @param name name of the definition in the capsule
     * @param defaultSize minimum size used if the capsule contains no non-null element at {@code name}
     * @return loaded PointerBuffer definition, or a completely new definition if no definition can be loaded
     * @throws IOException
     */
    public static CustomBufferDef<PointerBuffer> loadPointers(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, PointerBuffer.class, name, defaultSize);
    }

    /**
     * {@link #load(InputCapsule, Class, String, int) Loads} a CLongBuffer definition from an {@link InputCapsule}.
     *
     * @param in input capsule to load from
     * @param name name of the definition in the capsule
     * @param defaultSize minimum size used if the capsule contains no non-null element at {@code name}
     * @return loaded CLongBuffer definition, or a completely new definition if no definition can be loaded
     * @throws IOException
     */
    public static CustomBufferDef<CLongBuffer> loadCLongs(InputCapsule in, String name, int defaultSize) throws IOException {
        return load(in, CLongBuffer.class, name, defaultSize);
    }

    /**
     * Savable definition wrapper which saves {@link #setSize(int) size}, {@link #setPadding(int) padding},
     * and {@link #setInitToZero(boolean) initToZero} properties.
     *
     * @param <T>
     */
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

        /**
         * Creates a new definition with the specified size.
         *
         * @param size
         * @return
         */
        protected abstract CustomBufferDef<T> createDefinition(int size);

        /**
         * Gets the definition either loaded from an {@link InputCapsule} or to be saved to
         * an {@link OutputCapsule}.
         *
         * @return
         */
        public CustomBufferDef<T> getCustomBufferDef() {
            return def;
        }
        
    }

    /**
     * Saves definitions for {@link PointerBuffer PointerBuffers}.
     */
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

    /**
     * Saves definitions for {@link CLongBuffer CLongBuffers}.
     */
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
