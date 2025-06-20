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
package codex.renthyljme.effects.ports;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyljme.effects.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

/**
 *
 * @author codex
 */
public class ContrastAdjustmentPass extends AbstractFilterTask {

    private final ArgumentSocket<Float> redExponent = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> greenExponent = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> blueExponent = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> redScale = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> greenScale = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> blueScale = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> lowerLimit = new ArgumentSocket<>(this, 0f);
    private final ArgumentSocket<Float> upperLimit = new ArgumentSocket<>(this, 1f);

    public ContrastAdjustmentPass(AssetManager assetManager, ResourceAllocator allocator) {
        this(assetManager, allocator, 1f);
    }
    public ContrastAdjustmentPass(AssetManager assetManager, ResourceAllocator allocator, float exponent) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/ContrastAdjustment.j3md"), false);
        addSockets(redExponent, greenExponent, blueExponent, redScale, greenScale, blueScale, lowerLimit, upperLimit);
        setExponents(exponent, exponent, exponent);
    }

    @Override
    protected void configureMaterial(Material material) {
        redExponent.acquireToMaterial(material, "redChannelExponent");
        greenExponent.acquireToMaterial(material, "greenChannelExponent");
        blueExponent.acquireToMaterial(material, "blueChannelExponent");
        redScale.acquireToMaterial(material, "redChannelScale");
        greenScale.acquireToMaterial(material, "greenChannelScale");
        blueScale.acquireToMaterial(material, "blueChannelScale");
        lowerLimit.acquireToMaterial(material, "lowerLimit");
        upperLimit.acquireToMaterial(material, "upperLimit");
    }

    public void setExponents(float red, float green, float blue) {
        redExponent.setValue(red);
        greenExponent.setValue(green);
        blueExponent.setValue(blue);
    }

    public void setScale(float red, float green, float blue) {
        redScale.setValue(red);
        greenScale.setValue(green);
        blueScale.setValue(blue);
    }

    public void setLimits(float lower, float upper) {
        lowerLimit.setValue(lower);
        upperLimit.setValue(upper);
    }

    public ArgumentSocket<Float> getRedExponent() {
        return redExponent;
    }

    public ArgumentSocket<Float> getGreenExponent() {
        return greenExponent;
    }

    public ArgumentSocket<Float> getBlueExponent() {
        return blueExponent;
    }

    public ArgumentSocket<Float> getRedScale() {
        return redScale;
    }

    public ArgumentSocket<Float> getGreenScale() {
        return greenScale;
    }

    public ArgumentSocket<Float> getBlueScale() {
        return blueScale;
    }

    public ArgumentSocket<Float> getLowerLimit() {
        return lowerLimit;
    }

    public ArgumentSocket<Float> getUpperLimit() {
        return upperLimit;
    }

}
