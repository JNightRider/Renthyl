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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 *
 * @author codex
 */
public class LightScatteringPass extends AbstractFilterTask {

    private final Vector3f screenLightPos = new Vector3f();
    private final Vector3f viewLightPos = new Vector3f();

    private final ArgumentSocket<Vector3f> lightPosition = new ArgumentSocket<>(this);
    private final ArgumentSocket<Integer> samples = new ArgumentSocket<>(this, 50);
    private final ArgumentSocket<Float> blurStart = new ArgumentSocket<>(this, 0.02f);
    private final ArgumentSocket<Float> blurWidth = new ArgumentSocket<>(this, 0.9f);
    private final ArgumentSocket<Float> lightDensity = new ArgumentSocket<>(this, 1.4f);
    private final ArgumentSocket<Boolean> adaptive = new ArgumentSocket<>(this, true);

    public LightScatteringPass(AssetManager assetManager, ResourceAllocator allocator, Vector3f lightPosition) {
        super(allocator, new Material(assetManager, "Common/MatDefs/Post/LightScattering.j3md"), true);
        addSockets(samples, blurStart, blurWidth, lightDensity, adaptive);
        addSocket(this.lightPosition).setValue(lightPosition);
    }

    @Override
    protected void configureMaterial(Material material) {
        Camera cam = context.getCamera().getValue().getCamera();
        Vector3f lightPos = lightPosition.acquireOrThrow();
        getClipCoordinates(lightPos, screenLightPos, cam);
        cam.getViewMatrix().mult(lightPos, viewLightPos);
        float innerDensity = lightDensity.acquireOrThrow();
        if (adaptive.acquireOrThrow()) {
            float densityX = 1f - FastMath.clamp(FastMath.abs(screenLightPos.x - 0.5f), 0, 1);
            float densityY = 1f - FastMath.clamp(FastMath.abs(screenLightPos.y - 0.5f), 0, 1);
            innerDensity *= densityX * densityY;
        }
        samples.acquireToMaterial(material, "NbSamples");
        blurStart.acquireToMaterial(material, "BlurStart");
        blurWidth.acquireToMaterial(material, "BlurWidth");
        material.setVector3("LightPosition", screenLightPos);
        material.setFloat("LightDensity", innerDensity);
        material.setBoolean("Display", innerDensity != 0.0 && viewLightPos.z < 0);
    }

    private void getClipCoordinates(Vector3f worldPosition, Vector3f store, Camera cam) {
        float w = cam.getViewProjectionMatrix().multProj(worldPosition, store);
        store.divideLocal(w);
        store.x = ((store.x + 1f) * (cam.getViewPortRight() - cam.getViewPortLeft()) / 2f + cam.getViewPortLeft());
        store.y = ((store.y + 1f) * (cam.getViewPortTop() - cam.getViewPortBottom()) / 2f + cam.getViewPortBottom());
        store.z = (store.z + 1f) / 2f;
    }

    public ArgumentSocket<Vector3f> getLightPosition() {
        return lightPosition;
    }

    public ArgumentSocket<Integer> getSamples() {
        return samples;
    }

    public ArgumentSocket<Float> getBlurStart() {
        return blurStart;
    }

    public ArgumentSocket<Float> getBlurWidth() {
        return blurWidth;
    }

    public ArgumentSocket<Float> getLightDensity() {
        return lightDensity;
    }

    public ArgumentSocket<Boolean> getAdaptive() {
        return adaptive;
    }

}
