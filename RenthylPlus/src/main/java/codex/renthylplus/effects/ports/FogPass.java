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
import com.jme3.math.ColorRGBA;

/**
 *
 * @author codex
 */
public class FogPass extends AbstractFilterTask {

    private final ArgumentSocket<ColorRGBA> color = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> density = new ArgumentSocket<>(this);
    private final ArgumentSocket<Float> distance = new ArgumentSocket<>(this);

    /**
     * Creates a FogFilter
     */
    public FogPass(AssetManager assetManager, ResourceAllocator allocator) {
        this(assetManager, allocator, ColorRGBA.White.clone(), 0.7f, 1000f);
    }

    /**
     * Create a fog filter 
     * @param color the color of the fog (default is white)
     * @param density the density of the fog (default is 0.7)
     * @param distance the distance of the fog (default is 1000)
     */
    public FogPass(AssetManager assetManager, ResourceAllocator allocator, ColorRGBA color, float density, float distance) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/Fog.j3md"), true);
        addSocket(this.color).setValue(color);
        addSocket(this.density).setValue(density);
        addSocket(this.distance).setValue(distance);
    }

    @Override
    protected void configureMaterial(Material material) {
        color.acquireToMaterial(material, "FogColor");
        density.acquireToMaterial(material, "FogDensity");
        distance.acquireToMaterial(material, "FogDistance");
    }

    public ArgumentSocket<ColorRGBA> getColor() {
        return color;
    }

    public ArgumentSocket<Float> getDensity() {
        return density;
    }

    public ArgumentSocket<Float> getDistance() {
        return distance;
    }

}
