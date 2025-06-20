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
public class DepthOfFieldPass extends AbstractFilterTask {

    private final ArgumentSocket<Float> focusDistance = new ArgumentSocket<>(this, 50f);
    private final ArgumentSocket<Float> focusRange = new ArgumentSocket<>(this, 10f);
    private final ArgumentSocket<Float> blurScale = new ArgumentSocket<>(this, 1f);
    private final ArgumentSocket<Float> blurThreshold = new ArgumentSocket<>(this, 0.2f);
    private final ArgumentSocket<Boolean> debugUnfocus = new ArgumentSocket<>(this, false);

    public DepthOfFieldPass(AssetManager assetManager, ResourceAllocator allocator) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/DepthOfField.j3md"), true);
        addSockets(focusDistance, focusRange, blurScale, blurThreshold, debugUnfocus);
    }

    @Override
    protected void configureMaterial(Material material) {
        focusDistance.acquireToMaterial(material, "FocusDistance");
        focusRange.acquireToMaterial(material, "FocusRange");
        blurThreshold.acquireToMaterial(material, "BlurThreshold");
        debugUnfocus.acquireToMaterial(material, "DebugUnfocus");
        float scale = blurScale.acquireOrThrow();
        material.setFloat("XScale", scale / getResultDef().getWidth());
        material.setFloat("YScale", scale / getResultDef().getHeight());
    }

    public ArgumentSocket<Float> getFocusDistance() {
        return focusDistance;
    }

    public ArgumentSocket<Float> getFocusRange() {
        return focusRange;
    }

    public ArgumentSocket<Float> getBlurScale() {
        return blurScale;
    }

    public ArgumentSocket<Float> getBlurThreshold() {
        return blurThreshold;
    }

    public ArgumentSocket<Boolean> getDebugUnfocus() {
        return debugUnfocus;
    }

}
