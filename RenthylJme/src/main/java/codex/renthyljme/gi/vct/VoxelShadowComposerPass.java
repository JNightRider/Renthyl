/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.gi.vct;

import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.AbstractTask;
import codex.renthylplus.shadow.ShadowMap;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.Light;
import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;
import java.util.List;

/**
 *
 * @author codex
 */
public class VoxelShadowComposerPass extends AbstractTask {
    
    public static final int MAX_SHADOW_LIGHTS = 32;

    private final TransitiveSocket<Integer> gridSize = new TransitiveSocket<>(this);
    private final TransitiveSocket<BoundingBox> voxelBounds = new TransitiveSocket<>(this);
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final ValueSocket<Light[]> lightIndices = new ValueSocket<>(this);
    private final AllocationSocket<Texture3D> voxelLight;
    private final TextureDef<Texture3D> lightDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final Vector3f gridMin = new Vector3f();
    private final Vector3f gridMax = new Vector3f();
    private final GLComputeShader shader;
    private final WorkSize work = new WorkSize();
    private TextureImage lightImg;

    public VoxelShadowComposerPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(gridSize, voxelBounds, lightIndices, shadowMaps);
        voxelLight = addSocket(new AllocationSocket<>(this, allocator, lightDef));
        lightDef.setMagFilter(Texture.MagFilter.Nearest);
        lightDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        //lightDef.setAccess(Image.Access.ReadWrite);
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/VXGI/voxelShadowComposer.glsl");
    }

    @Override
    protected void renderTask() {

        int n = gridSize.acquireOrThrow("Grid size required.");
        lightDef.setCube(n);
        BoundingBox bound = voxelBounds.acquireOrThrow("Voxel bounds required.");
        bound.getMin(gridMin);
        bound.getMax(gridMax);
        
        if (lightImg == null) {
            lightImg = new TextureImage(voxelLight.acquire(), TextureImage.Access.ReadWrite);
        } else {
            lightImg.setTexture(voxelLight.acquire());
        }
        shader.set("VoxelLightMap", lightImg);
        shader.set("GridMin", gridMin);
        shader.set("GridMax", gridMax);

        List<ShadowMap> shadowMapList = shadowMaps.acquire();
        if (shadowMapList.isEmpty()) {
            throw new NullPointerException("No shadow maps provided.");
        }
        Light[] lightMap = new Light[Math.min(shadowMapList.size(), MAX_SHADOW_LIGHTS)];
        int nextLightIndex = 0;
        for (ShadowMap m : shadowMapList) {
            int i = indexOf(lightMap, m.getLight());
            if (i < 0) {
                i = nextLightIndex++;
                if (i >= MAX_SHADOW_LIGHTS) {
                    continue;
                }
                lightMap[i] = m.getLight();
            }
            shader.set("ShadowMap", m.getMap());
            shader.set("LightMatrix", m.getProjection());
            shader.set("LightIndex", i);
            shader.execute(work.set(n, 1).shiftToLocal(2));
        }

        lightIndices.setValue(lightMap);
        
    }

    public PointerSocket<Integer> getGridSize() {
        return gridSize;
    }

    public PointerSocket<BoundingBox> getVoxelBounds() {
        return voxelBounds;
    }

    public CollectorSocket<ShadowMap> getShadowMaps() {
        return shadowMaps;
    }

    public Socket<Light[]> getLightIndices() {
        return lightIndices;
    }

    public Socket<Texture3D> getVoxelLight() {
        return voxelLight;
    }

    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
            if (array[i] == null) {
                return -1;
            }
        }
        return -1;
    }
    
}
