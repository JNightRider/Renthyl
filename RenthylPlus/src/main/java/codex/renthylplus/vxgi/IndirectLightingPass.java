/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.jmecompute.Stride;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.uniforms.buffers.FloatArrayUniform;
import codex.renthyl.FrameGraphContext;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;

import java.util.Map;

/**
 *
 * @author codex
 */
public class IndirectLightingPass extends RenderTask {
    
    private static final float APERTURE_TAN = FastMath.tan(47f * FastMath.DEG_TO_RAD);
    private static final Vector2f SPEC_RANGE = new Vector2f(0.01f, 30f * FastMath.DEG_TO_RAD);
    private static final float[] tracePattern = {
        0, 0, 1, // normal-aligned
        1, 0, 1,
       -1, 0, 1,
        0, 1, 1,
        0,-1, 1,
    };

    private final SocketMap<String, TransitiveSocket<Texture2D>, Texture2D> gbufferMap = new SocketMap<>(this);
    private final TransitiveSocket<Texture3D> voxels = new TransitiveSocket<>(this);
    private final TransitiveSocket<Integer> gridSize = new TransitiveSocket<>(this);
    private final TransitiveSocket<BoundingBox> voxelBounds = new TransitiveSocket<>(this);
    private final ArgumentSocket<Float> traceQuality = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Vector2f> specularAngleRange = new ArgumentSocket<>(this, SPEC_RANGE);
    private final AllocationSocket<Texture2D> result;
    private final TextureDef<Texture2D> resultDef = TextureDef.texture2D(Image.Format.RGBA32F);
    private final Matrix4f camInverse = new Matrix4f();
    private final Vector3f gridMin = new Vector3f();
    private final Vector3f gridMax = new Vector3f();
    private final WorkSize work = new WorkSize();
    private final GLComputeShader shader;
    private TextureImage resultImg;
    private float time = 0;

    public IndirectLightingPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(gbufferMap, voxels, gridSize, voxelBounds, traceQuality, specularAngleRange);
        result = addSocket(new AllocationSocket<>(this, allocator, resultDef));
        gbufferMap.put("Color", new TransitiveSocket<>(this));
        gbufferMap.put("Depth", new TransitiveSocket<>(this));
        gbufferMap.put("Diffuse", new TransitiveSocket<>(this));
        gbufferMap.put("Position", new TransitiveSocket<>(this));
        gbufferMap.put("Normals", new TransitiveSocket<>(this));
        gbufferMap.put("Material", new TransitiveSocket<>(this));
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylPlus/MatDefs/VXGI/pbrIndirect.glsl");
        shader.uniform(new FloatArrayUniform("TraceDirections", Stride.Vec3)).set(tracePattern);
        shader.set("TraceTangent", APERTURE_TAN);
        shader.define("NUM_TRACES", tracePattern.length / 3);
    }

    @Override
    protected void renderTask() {
        
        Texture2D inColor = gbufferMap.get("Color").acquireOrThrow();
        int w = inColor.getImage().getWidth();
        int h = inColor.getImage().getHeight();
        resultDef.setSize(w, h);
        
        Camera cam = context.getViewPort().getCamera();
        cam.getViewProjectionMatrix().invert(camInverse);
        BoundingBox box = voxelBounds.acquire();
        box.getMin(gridMin);
        box.getMax(gridMax);
        
        if (resultImg == null) {
            resultImg = new TextureImage(result.acquire(), TextureImage.Access.WriteOnly);
        } else {
            resultImg.setTexture(result.acquire());
        }
        
        shader.set("ColorMap", inColor);
        shader.set("DepthMap", gbufferMap.get("Depth").acquireOrThrow());
        shader.set("DiffuseMap", gbufferMap.get("Diffuse").acquireOrThrow());
        shader.set("PositionMap", gbufferMap.get("Position").acquireOrThrow());
        shader.set("NormalMap", gbufferMap.get("Normals").acquireOrThrow());
        shader.set("MaterialMap", gbufferMap.get("Material").acquireOrThrow());
        shader.set("VoxelMap", voxels.acquireOrThrow());
        shader.set("CameraMatrixInverse", camInverse);
        shader.set("CameraPosition", cam.getLocation());
        shader.set("GridMin", gridMin);
        shader.set("GridMax", gridMax);
        shader.set("GridSize", gridSize.acquireOrThrow());
        shader.set("TraceQuality", traceQuality.acquireOrThrow());
        shader.set("SpecularAngleRange", specularAngleRange.acquireOrThrow());
        shader.set("IndirectFactor", 2.0f);
        shader.set("Target", resultImg);
        shader.set("Time", time);
        shader.execute(work.setGlobal(w, h, 1).setLocal(tracePattern.length/3 + 1, 1, 1));

        // TODO: remove this to not depend on the context
        time += context.getTpf();
        
    }

    public PointerSocket<Map<String, Texture2D>> getGBufferMap() {
        return gbufferMap;
    }

    public PointerSocket<Texture3D> getVoxels() {
        return voxels;
    }

    public PointerSocket<Integer> getGridSize() {
        return gridSize;
    }

    public PointerSocket<BoundingBox> getVoxelBounds() {
        return voxelBounds;
    }

    public ArgumentSocket<Float> getTraceQuality() {
        return traceQuality;
    }

    public ArgumentSocket<Vector2f> getSpecularAngleRange() {
        return specularAngleRange;
    }

    public Socket<Texture2D> getResult() {
        return result;
    }

}
