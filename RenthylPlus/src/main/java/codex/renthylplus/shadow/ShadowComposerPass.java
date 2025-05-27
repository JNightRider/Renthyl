/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.util.List;

/**
 *
 * @author gary
 */
public class ShadowComposerPass extends RenderTask {
    
    private static final int MAX_SHADOW_LIGHTS = 32;

    private final TransitiveSocket<Texture2D> sceneDepth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> sceneNormals = new TransitiveSocket<>(this);
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final AllocationSocket<Texture2D> lightContributions;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final ValueSocket<Light[]> lightShadowIndices = new ValueSocket<>(this);
    private final TextureDef<Texture2D> contributionDef = TextureDef.texture2D();
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final RenderState renderState = new RenderState();
    private final Material material;
    private final Vector2f tempInvRange = new Vector2f();
    private final CameraState camera = new CameraState(new Camera(1024, 1024), true);

    public ShadowComposerPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(sceneDepth, sceneNormals, shadowMaps, lightShadowIndices);
        lightContributions = addSocket(new AllocationSocket<>(this, allocator, contributionDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        contributionDef.setFormat(Image.Format.R32F);
        contributionDef.setMagFilter(Texture.MagFilter.Nearest);
        contributionDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        renderState.setBlendMode(RenderState.BlendMode.Off);
        renderState.setDepthTest(false);
        renderState.setDepthWrite(false);
        material = new Material(assetManager, "RenthylPlus/MatDefs/Shadows/ShadowCompose.j3md");
    }

    @Override
    protected void renderTask() {

        Texture2D depth = sceneDepth.acquireOrThrow("Scene depth required.");
        int w = depth.getImage().getWidth();
        int h = depth.getImage().getHeight();
        contributionDef.setSize(w, h);

        camera.resize(w, h, false);
        context.getCamera().pushValue(camera);

        bufferDef.setColorTargets(lightContributions.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        context.getForcedState().pushValue(renderState);
        material.setTexture("SceneDepthMap", depth);
        material.setMatrix4("CamViewProjectionInverse", context.getViewPort().getCamera().getViewProjectionMatrix().invert());
        boolean normals = sceneNormals.acquireToMaterial(material, "SceneNormalsMap") != null;
        
        // fullscreen render for each shadow map
        int nextIndex = 0;
        List<ShadowMap> maps = shadowMaps.acquire();
        Light[] indexMap = new Light[Math.min(maps.size(), MAX_SHADOW_LIGHTS)];
        lightShadowIndices.setValue(indexMap);
        for (ShadowMap m : maps) {
            if (m == null) {
                continue;
            }
            int i = indexOf(indexMap, m.getLight());
            if (i < 0 && nextIndex < MAX_SHADOW_LIGHTS) {
                i = nextIndex++;
                indexMap[i] = m.getLight();
            }
            if (i >= 0) {
                material.setTexture("ShadowMap", m.getMap());
                material.setMatrix4("LightViewProjectionMatrix", m.getProjection());
                material.setInt("LightType", m.getLight().getType().getId());
                material.setInt("LightIndex", i);
                material.setVector2("LightRange", m.getRange());
                if (normals) {
                    switch (m.getLight().getType()) {
                        case Directional: {
                            material.setVector3("LightPosition", ((DirectionalLight)m.getLight()).getDirection());
                        } break;
                        case Point: {
                            material.setVector3("LightPosition", ((PointLight)m.getLight()).getPosition());
                        } break;
                        case Spot: {
                            material.setVector3("LightPosition", ((SpotLight)m.getLight()).getPosition());
                        } break;
                        default: throw new UnsupportedOperationException("Shadows for " + m.getLight() + " are not supported.");
                    }
                }
                context.renderFullscreen(material);
                renderState.setBlendMode(RenderState.BlendMode.Additive);
            }
        }
        renderState.setBlendMode(RenderState.BlendMode.Off);

        context.getForcedState().pop();
        context.getFrameBuffer().pop();
        context.getCamera().pop();
        
    }
    
    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
        }
        return -1;
    }

    public PointerSocket<Texture2D> getSceneDepth() {
        return sceneDepth;
    }

    public PointerSocket<Texture2D> getSceneNormals() {
        return sceneNormals;
    }

    public CollectorSocket<ShadowMap> getShadowMaps() {
        return shadowMaps;
    }

    public Socket<Texture2D> getLightContribution() {
        return lightContributions;
    }

}
