/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.tasks.scene;

import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.geometry.GeometryRenderHandler;
import codex.renthyl.render.RenderEnvironment;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.RenderTask;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.List;

/**
 * Renders geometry to a color texture and a depth texture.
 *
 * <p>Optionally, input color and depth textures may be provided, and the geometry
 * will be rendered along side them. This can be used to essentially split rendering
 * among multiple GeometryPasses.</p>
 * 
 * @author codex
 */
public class GeometryPass extends RenderTask {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final Socket<Texture2D> outColor, outDepth;
    private final Socket<FrameBuffer> frameBuffer;
    private final PointerSocket<Texture2D> inColor = new TransitiveSocket<>(this);
    private final PointerSocket<Texture2D> inDepth = new TransitiveSocket<>(this);
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private RenderEnvironment env;
    private GeometryRenderHandler geometryHandler = GeometryRenderHandler.DEFAULT;

    public GeometryPass(ResourceAllocator allocator) {
        this(allocator, null);
    }
    public GeometryPass(ResourceAllocator allocator, RenderEnvironment env) {
        addSockets(geometry, inColor, inDepth);
        outColor = addSocket(new AllocationSocket<>(this, allocator, colorDef));
        outDepth = addSocket(new AllocationSocket<>(this, allocator, depthDef));
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        colorDef.setFormatFlexible(true);
        depthDef.setFormatFlexible(true);
    }

    @Override
    protected void renderTask() {

        // configure definitions
        colorDef.setSize(context.getWidth(), context.getHeight());
        depthDef.setSize(context.getWidth(), context.getHeight());
        bufferDef.setColorTargets(outColor.acquire());
        bufferDef.setDepthTarget(outDepth.acquire());

        FrameBuffer fb = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fb);
        context.clearBuffers();
        context.renderTextures(inColor.acquire(), inDepth.acquire());
        if (env != null) {
            env.applySettings(context);
        }
        List<GeometryQueue> queues = geometry.acquire();
        for (GeometryQueue q : queues) {
            q.applySettings(context);
            q.render(context, geometryHandler);
            q.restoreSettings(context);
        }
        if (env != null) {
            env.restoreSettings(context);
        }
        context.getFrameBuffer().pop();

    }

    /**
     * Sets the environment used when rendering.
     *
     * @param env
     */
    public void setEnvironment(RenderEnvironment env) {
        this.env = env;
    }

    /**
     * Sets the handler responsible for rendering.
     *
     * @param geometryHandler
     */
    public void setGeometryHandler(GeometryRenderHandler geometryHandler) {
        this.geometryHandler = geometryHandler;
    }

    /**
     * Gets the color texture resource definition.
     *
     * @return
     */
    public TextureDef<Texture2D> getColorDef() {
        return colorDef;
    }

    /**
     * Gets the depth texture resource definition.
     *
     * @return
     */
    public TextureDef<Texture2D> getDepthDef() {
        return depthDef;
    }

    /**
     * Gets the handler responsible for rendering.
     *
     * @return
     */
    public GeometryRenderHandler getGeometryHandler() {
        return geometryHandler;
    }

    /**
     * Gets socket accepting geometry queues to render (input).
     *
     * @return
     */
    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    /**
     * Gets socket for resulting color texture (output).
     *
     * @return
     */
    public Socket<Texture2D> getOutColor() {
        return outColor;
    }

    /**
     * Gets socket for resulting depth texture (output).
     *
     * @return
     */
    public Socket<Texture2D> getOutDepth() {
        return outDepth;
    }

    /**
     * Gets socket for overlayed color texture (input, optional).
     *
     * @return
     */
    public PointerSocket<Texture2D> getInColor() {
        return inColor;
    }

    /**
     * Gets socket for overlayed depth texture (input, optional).
     *
     * @return
     */
    public PointerSocket<Texture2D> getInDepth() {
        return inDepth;
    }

}
