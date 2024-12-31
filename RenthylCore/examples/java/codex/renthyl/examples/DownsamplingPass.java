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
package codex.renthyl.examples;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * A simple post-processing pass that will render an input texture
 * to a lower resolution output texture.
 * 
 * @author codex
 */
public class DownsamplingPass extends RenderPass {

    private ResourceTicket<Texture2D> in;
    private ResourceTicket<Texture2D> out;
    private TextureDef<Texture2D> texDef = TextureDef.texture2D();
    
    public DownsamplingPass() {
        this(Texture.MinFilter.NearestNoMipMaps, Texture.MagFilter.Nearest);
    }
    public DownsamplingPass(Texture.MinFilter min, Texture.MagFilter mag) {
        
        // Setup the output texture's min and mag filters.
        texDef.setMinFilter(min);
        texDef.setMagFilter(mag);
        
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        
        // Create the input ticket, which will allow this pass to receive
        // a texture from another pass.
        in = addInput("Input");
        
        // Create the output ticket, which will allow this pass to
        // give the result texture to another pass for further processing.
        out = addOutput("Output");
        
    }
    
    @Override
    protected void prepare(FGRenderContext context) {
        
        // Declare the output texture using the texture definition, indicating
        // we want to create a texture during execution.
        declare(texDef, out);
        
        // Reserve the resource used for output last time. Greatly
        // increases the chances of getting the same texture from
        // frame to frame, which minimizes texture binds (expensive).
        reserve(out);
        
        // Reference the input texture, indicating we want to use the
        // input texture during execution.
        reference(in);
        
    }
    
    @Override
    protected void execute(FGRenderContext context) {
        
        // Acquire the input texture. An exception is thrown if it cannot be found.
        Texture2D inTex = resources.acquire(in);
        Image img = inTex.getImage();
        
        // Set the output texture's demensions to half the input texture's demensions.
        int w = img.getWidth() / 2;
        int h = img.getHeight() / 2;
        texDef.setSize(w, h);
        
        // Set the ouptut texture's format to the input texture's format.
        texDef.setFormat(img.getFormat());
        
        // Resize the camera to the output texture's size. Otherwise only a corner
        // of the input texture will be rendered to the output texture. Registering
        // as a mode guarantees that the camera size will be reverted after execution.
        context.registerMode(RenderMode.cameraSize(w, h));
        
        // Get a FrameBuffer to render to that is the same size as the output texture.
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        
        // Acquire the output texture and attach it to the FrameBuffer as a color target.
        // This method is designed to minimize texture binds.
        resources.acquireColorTarget(fb, out);
        
        // Setup the FrameBuffer for rendering. Again, registering this change as
        // as a mode guarantees that it will be reverted after execution.
        context.registerMode(RenderMode.frameBuffer(fb));
        context.clearBuffers();
        
        // Render the input texture to the output texture.
        context.renderTextures(inTex, null);
        
    }
    
    @Override
    protected void reset(FGRenderContext context) {
        // No reset needed.
    }
    
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        // No cleanup needed.
    }
    
    @Override
    protected void write(OutputCapsule out) throws IOException {
        
        // Save the texture definition
        out.write(TextureDef.saveTexture2D(texDef), "texDef", null);
        
    }
    
    @Override
    protected void read(InputCapsule in) throws IOException {
        
        // Read the saved texture definition
        texDef = TextureDef.readTexture2D(in, "texDef", null);
        
    }
    
}
