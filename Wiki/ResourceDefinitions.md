# Resource Definitions

### TextureDef

General-use definition for any texture resources. Static helper methods are available for Texture2D and Texture3D.

```java
TextureDef<Texture2D> texDef1 = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
TextureDef<Texture2D> texDef2 = TextureDef.texture2D();
TextureDef<Texture3D> texDef3 = TextureDef.texture3D();
```

On allocation, the contents of the texture are not guaranteed.

### BufferDef

General-use definition for Buffers. Any Buffer implementation is supported, but helper methods are provided for ByteBuffer, IntBuffer, FloatBuffer, DoubleBuffer, LongBuffer, and ShortBuffer.

```java
BufferDef bufDef1 = new BufferDef(
        IntBuffer.class, n -> BufferUtils.createIntBuffer(n), 10);
BufferDef bufDef2 = BufferDef.ints(10);
BufferDef bufDef3 = BufferDef.floats(10);
```

The integer argument determines the minimum size of the buffer. A buffer must be at least the minimum size (capacity) to be accepted by BufferDef for reallocation. Buffers created by BufferDef will be exactly the minimum size plus a padding integer, which is zero by default and useful to set higher if the BufferDef's minimum size changes often.

When a buffer is allocated using a BufferDef, the BufferDef will set the buffer's position to zero and its limit to the BufferDef's minimum size. Additionally, BufferDef can initialize all values in a buffer to zero at this time, but this is disabled by default.

On allocation, the contents of the buffer are not guaranteed.

### CustomBufferDef

Similar to BufferDef, but is intended for LWJGL's CustomBuffers which are not implementations of Buffer. All other functionality is the same as BufferDef.

### ArrayDef

Similar to BufferDef, but is intended for Object arrays rather than Buffers.

```java
ArrayDef<Vector3f> arrayDef = new ArrayDef(Vector3f.class, 10);
```

For primitive type arrays, FloatArrayDef and IntArrayDef are provided. Further implementations of such may be created by extending AbstractArrayDef.
