# Resource Definitions

ResourceDef is an interface that defines how the resource system should handle a particular resource. ResourceDef provides many methods for returning option states, but it primarily concerned with creating new resources and approving existing resources for allocation. When a ResourceDef is used in resource declaration, the current properties of the ResourceDef become embedded in the resource's metadata.

`ResourceDef#createResource` is relatively self-explanatory. `ResourceDef#applyDirectResource` and `ResourceDef#applyIndirectResource` handle two different possibilities of resource allocation.

`applyDirectResource` tests if the resources *closely matches* or is preferred based on the ResourceDefs internal properties. The return value of this method, if not null, is immediately reallocated by the resource system.

`applyIndirectResource` tests if the resource *can be used* but is not preferred. The return value, if not null, is saved by the resource system for reallocation in case no resources can be reallocated directly via `applyDirectResource`. This is, in practice, not used very often, but it is useful for some situations. For example, a specific slice of a 3D texture can be reallocated as an indirect 2D texture resource. Obviously a real 2D texture is preferred, hence the 3D texture only being considered indirectly.

Another set of methods that are not directly options are `getResourceTag` and `isEquivalentTag`. The idea is to be able to narrow the reallocation scope by only accepting resources with a similar tag.

The remaining methods provided by ResourceDef expose options that can be used to tweak how resources are handled in specific cases.

* **getStaticTimeout.** Defines the number of complete render frames a resource can survive without being accessed. If the number of inactive frames exceeds this amount, the resource is disposed.
* **getDisposalMethod.** Returns a Consumer which is expected to adequetely "de-initialize" or dispose the resource.
* **isUseExisting.** If true, the resource system can completely skip the reallocation process in favor of creating a new resource. In short, if true, the ResourceDef denies explicitely denies all reallocation attempts.
* **isAllowCasualAllocation.** This disables the resource system from searching for a resource to reallocate. A resource is only reallocated if it is specifically pointed out by a resource ID (called *specific allocation*).
* **isAllowReservations.** If true, the managed resource may accept reservations.
* **isDisposeOnRelease.** If true, the resource will immediately be disposed once it has been totally released. This would be more in tune with traditionally frame graph management.
* **isReadConcurrent.** If true, the resource may be read by multiple modules simultaneously. Otherwise additional work must be done to block execution threads from accessing the same resource at once.
* **IsAllowIndirectResources.** If false, the resource system will *not* test indirect resource allocations via `applyIndirectResource`. This is functionally the same as having `applyIndirectResource` always return null.

AbstractResourceDef provides setters for all of the above options.

### TextureDef

General-use ResourceDef implementation for any texture resources. Static helper methods are available for Texture2D and Texture3D.

```java
TextureDef<Texture2D> texDef1 = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
TextureDef<Texture2D> texDef2 = TextureDef.texture2D();
TextureDef<Texture3D> texDef3 = TextureDef.texture3D();
```

On allocation, the contents of the allocated texture are not guaranteed.

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
