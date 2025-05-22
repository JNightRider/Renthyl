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

import codex.renthyl.FrameGraphContext;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.Socket;
import codex.renthylplus.effects.AbstractFilterTask;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

/**
 *
 * @author codex
 */
public class PosterizationPass extends AbstractFilterTask {

    private final ArgumentSocket<Integer> colors = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> gamma = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> strength = new ArgumentSocket<>(this, 1.0f);

    public PosterizationPass(AssetManager assetManager, ResourceAllocator allocator) {
        this(assetManager, allocator, 8);
    }
    public PosterizationPass(AssetManager assetManager, ResourceAllocator allocator, int colors) {
        this(assetManager, allocator, colors, 0.6f);
    }
    public PosterizationPass(AssetManager assetManager, ResourceAllocator allocator, int colors, float gamma) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/Posterization.j3md"), false);
        addSocket(this.colors).setValue(colors);
        addSocket(this.gamma).setValue(gamma);
        addSocket(strength);
    }

    @Override
    protected void configureMaterial(Material material) {
        colors.acquireToMaterial(material, "NumColors");
        gamma.acquireToMaterial(material, "Gamma");
        strength.acquireToMaterial(material, "Strength");
    }

    public ArgumentSocket<Integer> getColors() {
        return colors;
    }

    public ArgumentSocket<Float> getGamma() {
        return gamma;
    }

    public ArgumentSocket<Float> getStrength() {
        return strength;
    }

}
