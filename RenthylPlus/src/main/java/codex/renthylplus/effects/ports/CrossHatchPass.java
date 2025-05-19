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
package codex.renthylplus.effects.ports;

import codex.renthyl.render.Renderable;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthylplus.effects.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

/**
 *
 * @author codex
 */
public class CrossHatchPass extends AbstractFilterTask {

    private final ArgumentSocket<ColorRGBA> lineColor = new ArgumentSocket<>(this);
    private final ArgumentSocket<ColorRGBA> paperColor = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> colorInfluenceLine = new ArgumentSocket<>(this, 0.8f);
    private final ArgumentSocket<Float> colorInfluencePaper = new ArgumentSocket<>(this, 0.1f);
    private final ArgumentSocket<Float> fillValue = new ArgumentSocket<>(this, 0.9f);
    private final ArgumentSocket<Float> lineThickness = new ArgumentSocket<>(this, 1.0f);
    private final ArgumentSocket<Float> lineDistance = new ArgumentSocket<>(this, 4.0f);
    private final LuminanceSocket luminance = new LuminanceSocket(this, 0.9f, 0.7f, 0.5f, 0.3f, 0.0f);

    /**
     * Creates a crossHatch filter
     */
    public CrossHatchPass(AssetManager assetManager, ResourceAllocator allocator) {
        this(assetManager, allocator, ColorRGBA.Black.clone(), ColorRGBA.White.clone());
    }

    /**
     * Creates a crossHatch filter
     * @param lineColor the colors of the lines
     * @param paperColor the paper color
     */
    public CrossHatchPass(AssetManager assetManager, ResourceAllocator allocator, ColorRGBA lineColor, ColorRGBA paperColor) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/CrossHatch.j3md"), false);
        addSockets(this.lineColor, this.paperColor, colorInfluenceLine, colorInfluencePaper, fillValue, lineThickness, lineDistance, luminance);
        this.lineColor.setValue(lineColor);
        this.paperColor.setValue(paperColor);
    }

    @Override
    protected void configureMaterial(Material material) {
        lineColor.acquireToMaterial(material, "LineColor");
        paperColor.acquireToMaterial(material, "PaperColor");
        colorInfluenceLine.acquireToMaterial(material, "ColorInfluenceLine");
        colorInfluencePaper.acquireToMaterial(material, "ColorInfluencePaper");
        fillValue.acquireToMaterial(material, "FillValue");
        lineThickness.acquireToMaterial(material, "LineThickness");
        lineDistance.acquireToMaterial(material, "LineDistance");
        float[] lum = luminance.acquireOrThrow();
        material.setFloat("Luminance1", lum[0]);
        material.setFloat("Luminance2", lum[1]);
        material.setFloat("Luminance3", lum[2]);
        material.setFloat("Luminance4", lum[3]);
        material.setFloat("Luminance5", lum[4]);
    }

    public ArgumentSocket<ColorRGBA> getLineColor() {
        return lineColor;
    }

    public ArgumentSocket<ColorRGBA> getPaperColor() {
        return paperColor;
    }

    public ArgumentSocket<Float> getColorInfluenceLine() {
        return colorInfluenceLine;
    }

    public ArgumentSocket<Float> getColorInfluencePaper() {
        return colorInfluencePaper;
    }

    public ArgumentSocket<Float> getFillValue() {
        return fillValue;
    }

    public ArgumentSocket<Float> getLineThickness() {
        return lineThickness;
    }

    public ArgumentSocket<Float> getLineDistance() {
        return lineDistance;
    }

    public LuminanceSocket getLuminance() {
        return luminance;
    }

    public static class LuminanceSocket extends ArgumentSocket<float[]> {

        public LuminanceSocket(Renderable task, float... values) {
            super(task);
            super.setValue(values);
        }

        @Override
        public void setValue(float... values) {
            if (values.length != 6) {
                throw new IllegalArgumentException("Luminance requires 5 values.");
            }
            super.setValue(values);
        }

        public void setValue(int i, float l) {
            getValue()[i] = l;
        }

    }
    
}
