/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthylplus.effects;

import codex.renthyl.FrameGraphContext;
import codex.renthyl.FrameGraph;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class GaussianBlurPass extends JmeFilterPass {
    
    private float blurScale = 1.5f;
    private float downSamplingFactor = 1;
    private Material vBlurMat;
    private Material hBlurMat;
    private int screenWidth;
    private int screenHeight;
    
    public GaussianBlurPass() {}

    @Override
    protected void init(FrameGraph frameGraph) {

        //configuring horizontal blur pass
        hBlurMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Blur/HGaussianBlur.j3md");
        Subpass hBlurPass = add(new Subpass(hBlurMat, true, false) {
            @Override
            public void beforeAcquire(FrameGraphContext context) {
                getDef().setSize(screenWidth, screenHeight);
            }
            @Override
            public void beforeRender(FrameGraphContext context) {
                hBlurMat.setFloat("Size", screenWidth);
                hBlurMat.setFloat("Scale", blurScale);
            }
        });

        //configuring vertical blur pass
        vBlurMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Blur/VGaussianBlur.j3md");
        Subpass vBlurPass = add(new Subpass(vBlurMat, false, false) {
            @Override
            public void beforeAcquire(FrameGraphContext context) {
                getDef().setSize(screenWidth, screenHeight);
            }
            @Override
            public void beforeRender(FrameGraphContext context) {
                vBlurMat.setTexture("Texture", hBlurPass.getRenderedTexture());
                vBlurMat.setFloat("Size", screenHeight);
                vBlurMat.setFloat("Scale", blurScale);
            }
        });
        
    }
    @Override
    protected void execute(FrameGraphContext context) {
        // multiple acquire calls with the same ticket is perfectly fine
        Texture2D inColor = resources.acquire(sceneColor);
        screenWidth = (int)Math.max(1, (inColor.getImage().getWidth() / downSamplingFactor));
        screenHeight = (int)Math.max(1, (inColor.getImage().getHeight() / downSamplingFactor));
        super.execute(context);
    }

    /**
     * returns the blur scale
     * @return the blur scale
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * sets The spread of the bloom default is 1.5f
     *
     * @param blurScale the desired scale (default=1.5)
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * returns the downSampling factor<br>
     * for more details see {@link #setDownSamplingFactor(float downSamplingFactor)}
     * @return the downsampling factor
     */
    public float getDownSamplingFactor() {
        return downSamplingFactor;
    }

    /**
     * Sets the downSampling factor : the size of the computed texture will be divided by this factor. default is 1 for no downsampling
     * A 2 value is a good way of widening the blur
     *
     * @param downSamplingFactor the desired factor (default=1)
     */
    public void setDownSamplingFactor(float downSamplingFactor) {
        this.downSamplingFactor = downSamplingFactor;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(blurScale, "blurScale", 1.5f);
        oc.write(downSamplingFactor, "downSamplingFactor", 1);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        blurScale = ic.readFloat("blurScale", 1.5f);
        downSamplingFactor = ic.readFloat("downSamplingFactor", 1);
    }
    
}
