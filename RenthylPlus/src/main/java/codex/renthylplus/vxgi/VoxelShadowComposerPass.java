/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.vxgi;

import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthylplus.shadow.ShadowMap;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.Light;
import com.jme3.math.Vector3f;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class VoxelShadowComposerPass extends RenderPass {
    
    public static final int MAX_SHADOW_LIGHTS = 32;
    
    private ResourceTicket<Integer> gridSize;
    private ResourceTicket<BoundingBox> voxelBounds;
    private ResourceTicket<Texture3D> voxelLight;
    private ResourceTicket<Light[]> lightShadowIndices;
    private DynamicTicketList<ShadowMap> shadowMaps;
    private final TextureDef<Texture3D> lightDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final LinkedList<ShadowMap> shadowMapList = new LinkedList<>();
    private final Vector3f gridMin = new Vector3f();
    private final Vector3f gridMax = new Vector3f();
    private final WorkSize work = new WorkSize();
    private TextureImage lightImg;
    private GLComputeShader shader;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        gridSize = addInput("GridSize");
        voxelBounds = addInput("Bounds");
        shadowMaps = addInputGroup(new DynamicTicketList<>("ShadowMaps"));
        voxelLight = addOutput("LightContribution");
        lightShadowIndices = addOutput("LightShadowIndices");
        lightDef.setMagFilter(Texture.MagFilter.Nearest);
        lightDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        //lightDef.setAccess(Image.Access.ReadWrite);
        shader = UniversalShaderLoader.loadComputeShader(frameGraph.getAssetManager(),
                "RenthylPlus/MatDefs/VXGI/voxelShadowComposer.glsl");
        shader.uniformImage("VoxelLightMap");
        shader.uniformsVector3("GridMin", "GridMax");
        shader.uniformTexture("ShadowMap");
        shader.uniformMatrix4("LightMatrix");
        shader.uniformInt("LightIndex");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(lightDef, voxelLight);
        declarePrimitive(lightShadowIndices);
        reference(gridSize, voxelBounds);
        reference(shadowMaps);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        int n = resources.acquire(gridSize);
        lightDef.setCube(n);
        BoundingBox bound = resources.acquire(voxelBounds);
        bound.getMin(gridMin);
        bound.getMax(gridMax);
        
        if (lightImg == null) {
            lightImg = new TextureImage(resources.acquire(voxelLight), TextureImage.Access.ReadWrite);
        } else {
            lightImg.setTexture(resources.acquire(voxelLight));
        }
        shader.set("VoxelLightMap", lightImg);
        shader.set("GridMin", gridMin);
        shader.set("GridMax", gridMax);
        
        acquireList(shadowMaps, shadowMapList);
        if (shadowMapList.isEmpty()) {
            throw new NullPointerException("No shadow maps provided.");
        }
        Light[] lightMap = new Light[Math.min(shadowMaps.size(), MAX_SHADOW_LIGHTS)];
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
        
        resources.setPrimitive(lightShadowIndices, lightMap);
        
    }
    @Override
    protected void reset(FGRenderContext context) {
        shadowMapList.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
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
