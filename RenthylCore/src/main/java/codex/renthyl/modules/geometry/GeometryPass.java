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
package codex.renthyl.modules.geometry;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * Renders a queue bucket to a set of color and depth textures.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Geometry ({@link GeometryQueue}): queue of geometries to render.</li>
 *   <li>Color ({@link Texture2D}): Color texture to combine (by depth comparison) with the result of this render (optional).</li>
 *   <li>Depth ({@link Texture2D}): Depth texture to combine (by depth comparison) with the result of this render (optional).</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): Resulting color texture.</li>
 *   <li>Depth ({@link Texture2D}): Resulting depth texture.</li>
 * </ul>
 * 
 * @author codex
 */
public class GeometryPass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor, inDepth, outColor, outDepth;
    private ResourceTicket<GeometryQueue> geometry;
    private TextureDef<Texture2D> colorDef, depthDef;
    private GeometryRenderHandler geometryHandler = GeometryRenderHandler.DEFAULT;
    
    public GeometryPass() {}
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        inDepth = addInput("Depth");
        geometry = addInput("Geometry");
        outColor = addOutput("Color");
        outDepth = addOutput("Depth");
        colorDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        depthDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img), Image.Format.Depth);
        colorDef.setFormatFlexible(true);
        depthDef.setFormatFlexible(true);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        colorDef.setSize(w, h);
        depthDef.setSize(w, h);
        declare(colorDef, outColor);
        declare(depthDef, outDepth);
        reserve(outColor, outDepth);
        reference(geometry);
        referenceOptional(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, outColor);
        resources.acquireDepthTarget(fb, outDepth);
        //context.registerMode(RenderMode.background(ColorRGBA.BlackNoAlpha));
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        //System.out.println(inColor.getWorldIndex() + "   " + inColor.getLocalIndex());
        //System.out.println(resources.acquireOrElse(inColor, null) + "    " + resources.acquireOrElse(inDepth, null));
        context.renderTextures(resources.acquireOrElse(inColor, null), resources.acquireOrElse(inDepth, null));
        resources.acquire(geometry).render(context, geometryHandler);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}

    public void setGeometryHandler(GeometryRenderHandler geometryHandler) {
        this.geometryHandler = geometryHandler;
    }
    
    public TextureDef<Texture2D> getColorDef() {
        return colorDef;
    }

    public TextureDef<Texture2D> getDepthDef() {
        return depthDef;
    }
    
    public GeometryRenderHandler getGeometryHandler() {
        return geometryHandler;
    }
    
}
