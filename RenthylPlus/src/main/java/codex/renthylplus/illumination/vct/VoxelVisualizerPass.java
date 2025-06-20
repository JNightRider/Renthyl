/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.illumination.vct;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.tasks.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;

/**
 *
 * @author codex
 */
public class VoxelVisualizerPass extends RasterTask {

    private final TransitiveSocket<Texture3D> voxels = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> geometry = new TransitiveSocket<>(this);
    private final TransitiveSocket<BoundingBox> voxelBounds = new TransitiveSocket<>(this);
    private final AllocationSocket<Texture2D> color, depth;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final Material material;

    public VoxelVisualizerPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(voxels, geometry, voxelBounds);
        color = addSocket(new AllocationSocket<>(this, allocator, colorDef));
        depth = addSocket(new AllocationSocket<>(this, allocator, depthDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        material = new Material(assetManager, "RenthylJme/MatDefs/VXGI/voxelDebug.j3md");
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
    }

    @Override
    protected void renderTask() {
        
        colorDef.setSize(context.getWidth(), context.getHeight());
        depthDef.setSize(colorDef);

        bufferDef.setColorTargets(color.acquireOrThrow());
        bufferDef.setDepthTarget(depth.acquireOrThrow());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();
        
        BoundingBox box = voxelBounds.acquireOrThrow();
        Vector3f min = box.getMin(new Vector3f());
        Vector3f max = box.getMax(new Vector3f());
        material.setTexture("VoxelMap", voxels.acquireOrThrow());
        material.setVector3("GridMin", min);
        material.setVector3("GridMax", max);
        context.getForcedMaterial().pushValue(material);

        geometry.acquireOrThrow().render(context);

        context.getForcedMaterial().pop();
        context.getFrameBuffer().pop();
        
    }
    
}
