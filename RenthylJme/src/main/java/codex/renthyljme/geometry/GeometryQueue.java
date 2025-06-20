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
package codex.renthyljme.geometry;

import codex.renthyljme.FrameGraphContext;
import codex.renthyl.render.RenderEnvironment;
import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Geometry;

/**
 * Queue of geometries for rendering.
 *
 * <p>Implementations are free to render and iterate geometries in any order. Most commonly,
 * geometries are rendered in an order determined by a {@link com.jme3.renderer.queue.GeometryComparator},
 * and iterated in the order they were added.</p>
 *
 * @author codex
 */
public interface GeometryQueue extends RenderEnvironment, Iterable<Geometry> {

    /**
     * Adds the geometry to the queue.
     *
     * @param g
     */
    void add(Geometry g);

    /**
     * Renders geometries from the queue.
     *
     * <p>Not all geometries are guaranteed to be rendered due to culling. Culling is evaluated
     * using {@code handler}.</p>
     *
     * @param context
     * @param handler rendering handler (not null)
     * @return number of geometries rendered
     */
    int render(FrameGraphContext context, GeometryRenderHandler handler);

    /**
     * Gets the number of geometries in this queue.
     *
     * @return
     */
    int size();

    /**
     * Clears all geometries from this queue and performs any other
     * implementation-specific reseting.
     */
    void clear();

    /**
     * Renders geometries from the queue using the {@link GeometryRenderHandler#DEFAULT default}
     * handler.
     *
     * @param context
     * @return number of geometries rendered
     */
    default int render(FrameGraphContext context) {
        return render(context, GeometryRenderHandler.DEFAULT);
    }

    /**
     * Computes a {@link BoundingBox} encompassing all iterated geometries.
     *
     * @param geometry
     * @return
     */
    static BoundingBox computeBounds(Iterable<Geometry> geometry) {
        BoundingBox box = new BoundingBox();
        for (Geometry g : geometry) {
            box.mergeLocal(g.getWorldBound());
        }
        return box;
    }

}
