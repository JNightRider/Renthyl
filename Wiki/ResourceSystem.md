# Resource System

### Optional Methods

ResourceList provides many methods named "optional" (i.e. referenceOptional). Such methods do not expect a "valid" ResourceTicket to be passed. If a given ResourceTicket is invalid (i.e. null or not pointing to an existing resource), the operation is simply skipped. Non-optional methods will throw exceptions if a ResourceTicket is invalid.

Note that `acquireOrElse` is an optional method, although it is not named "optional."

### Primitive ResourceViews

Declaring a ResourceView as a primitive through `Resource#declarePrimitive` allows modules to directly assign the resource of the ResourceView instead of using a ResourceDef. This bypasses the RenderObjectMap and makes handling the resource much cheaper, so this state is suitable for resources that don't benefit much from RenderObjectMap (i.e. integers).

```java
@Override
protected void prepare(FGRenderContext context) {
    declarePrimitive(myTicket);
}

@Override
protected void execute(FGRenderContext context) {
    resources.setPrimitive(myTicket, myValue);
}
```

Note that no ResourceDef is required to declare a primitive ResourceView. For primitive ResourceViews, `setPrimitive` should be called before acquiring, otherwise an exception will occur because no ResourceDef is defined.

### Temporary ResourceViews

For resources that are neither input nor output, the ResourceView should be declared as temporary. Temporary ResourceViews cannot be referenced, but they do not affect culling and they are private to the module that declares them.

```java
private final ResourceTicket<Texture2D> tempTex = new ResourceTicket<>("tempTex");
private final TextureDef<Texture2D> tempTexDef = ...

@Override
protected void prepare(FGRenderContext context) {
    declareTemporary(tempTexDef, tempTex);
}

@Override
protected void execute(FGRenderContext context) {
    Texture2D temp = resources.acquire(tempTex);
    ...
    // after finished with "temp", manually release the resource
    resources.release(temp);
}
```

Note that the ResourceTicket corresponding to the temporary ResourceView should not be registered as either an input or output ticket. Also note that the resource must be manually released after the resource is used. This is done automatically for input and output resources, but not for temporary resources.

### Reserving

Sometimes, especially for textures, it is desirable to acquire the same resource from frame-to-frame. Reserving a resource ensures that no other modules reallocate the resource so that it is unavailable when needed. A general rule-of-thumb is to always reserve textures that are attached to framebuffers.

```java
@Override
protected void prepare(FGRenderContext context) {
    declare(myDef, myTicket);
    reserve(myTicket);
}
```

ResourceTickets store the ID of the last used resource. `reserve` uses that ID to locate the resource and submit a reservation based on when the module is scheduled to execute. Reserving should not be absolutely relied on to return the expected results, as some circumstances can result in the resource being unavailable at the reserved time, however, if the resource is available it will always be chosen.

### Undefined ResourceViews

An undefined ResourceView can never be associated with a resource, and non-optional operations on it will result in an exception. This is mainly used when the declaring module is unable to fufill the ResourceView with a meaningful resource, and thus marking it as undefined will not allow other passes to accidentally create and use a garbage resource when they go to acquire it. For example, [Attribute](Modules.md#attribute) marks its output as undefined if a GraphSource is not provided.

```java
@Override
protected void execute(FGRenderContext context) {
    resources.setUndefined(myTicket);
}
```

If the ResourceView is already associated with a resource when `setUndefined` is called, an exception is thrown.

### Constant Resources

When a resource is marked as constant, it cannot be reallocated to any ResourceView for the remainder of the frame. This is used to ensure that a resource doesn't unexpectedly change if a resource is shared with something outside the FrameGraph context. For example, [Attribute](Modules.md#attribute) marks its input resource as constant because the resource is shared outside the FrameGraph through a GraphTarget.

```java
@Override
protected void execute(FGRenderContext context) {
    resources.setConstant(myTicket);
}
```

Note that this operation, unlike setting a ResoureView as undefined, alters a property of the resource itself.

### Virtual ResourceViews

A ResourceView is virtual when it is not associated with a resource and it is not undefined. Being virtual is simply a state a ResourceView can be in.

### Modify

`ResourceList#modify` transfers a resource from one ResourceView to another by essentially releasing it from the former and immediately allocating it to the latter. This is useful for a module is intended to modify an input resource and produce it as an output resource.

```java
@Override
protected void execute(FGRenderContext context) {
    Texture2D tex = resources.modify(inputTicket, outputTicket);
}
```

`modify` is intended to replace an `acquire` call. This particular operation requires a very specific situation in order to not throw an exception.

* Both tickets must be valid (non-null and pointing to existing resource).
* The source must not be virtual or undefined (it must hold a resource).
* The target must be virtual (it cannot hold a resource or be undefined).
* The source must have exactly one active reference to it (in other words, only the current module should be actively referencing it).
* The resource must not be destroyed when it is released.
* The resource cannot be constant.
* The target's ResourceDef must accept the resource.
