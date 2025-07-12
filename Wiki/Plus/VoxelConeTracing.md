# Voxel Cone Tracing

Voxel cone tracing (VCT) is a global illumination technique that first writes scene geometry into a 3D texture representing voxels, then traces cones over that 3D texture to approximate indirect lighting. RenthylPlus provides a set of modules to execute this technique.

## Using the VoxelConeTracer Module

Each RenderPass implementation for performing voxel cone tracing is wrapped in the VoxelConeTracer module, which extends RenderContainer. VCT can be performed without this module, but the underlying modules must be set up by hand.

```java
FrameGraph fg = ...
VoxelConeTracer vct = fg.add(new VoxelConeTracer()).create();
```

The `create` call has the VoxelConeTracer create its internal modules and connections. Otherwise the VoxelConeTracer would be completely empty.

### Shadows

> The current version (1.2.5-alpha) requires that at least one shadow-casting light be in the scene. This, of course, should not be the case later on.


