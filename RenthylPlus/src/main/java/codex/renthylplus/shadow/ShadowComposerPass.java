/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.shadow;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.tasks.RenderTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector2f;
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

    private final TransitiveSocket<Texture2D> receiverDepth = new TransitiveSocket<>(this);
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final AllocationSocket<Texture2D> lightContributions;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final ValueSocket<Light[]> lightShadowIndexSocket = new ValueSocket<>(this);
    private final TextureDef<Texture2D> contributionDef = TextureDef.texture2D();
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final RenderState renderState = new RenderState();
    private final Material material;
    private final Vector2f tempInvRange = new Vector2f();

    public ShadowComposerPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(receiverDepth, shadowMaps, lightShadowIndexSocket);
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
        
        int w = context.getWidth();
        int h = context.getHeight();
        contributionDef.setSize(w, h);

        bufferDef.setColorTargets(lightContributions.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        context.getForcedState().pushValue(renderState);
        material.setTexture("SceneDepthMap", receiverDepth.acquireOrThrow());
        material.setMatrix4("CamViewProjectionInverse", context.getViewPort().getCamera().getViewProjectionMatrix().invert());
        
        // fullscreen render for each shadow map
        int nextIndex = 0;
        List<ShadowMap> maps = shadowMaps.acquire();
        Light[] indexMap = new Light[Math.min(maps.size(), MAX_SHADOW_LIGHTS)];
        lightShadowIndexSocket.setValue(indexMap);
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
                material.setVector2("LightRangeInverse", m.getInverseRange(tempInvRange));
                context.renderFullscreen(material);
                renderState.setBlendMode(RenderState.BlendMode.Additive);
            }
        }
        renderState.setBlendMode(RenderState.BlendMode.Off);

        context.getForcedState().pop();
        context.getFrameBuffer().pop();
        
    }
    
    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
        }
        return -1;
    }

    public TransitiveSocket<Texture2D> getReceiverDepth() {
        return receiverDepth;
    }

    public CollectorSocket<ShadowMap> getShadowMaps() {
        return shadowMaps;
    }

    public Socket<Texture2D> getLightContribution() {
        return lightContributions;
    }

}
