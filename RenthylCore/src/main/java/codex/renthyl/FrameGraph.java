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
package codex.renthyl;

import codex.renthyl.render.queue.BasicRenderingQueue;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.SynchronizedAttribute;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.renderer.ViewPort;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class FrameGraph extends ArrayList<Renderable> implements RenderPipeline<FrameGraphContext> {

    private final AssetManager assetManager;
    private final RenderingQueue queue;
    private final SynchronizedAttribute<FrameGraphContext> contextAttr = new SynchronizedAttribute<>();
    private boolean rendered = false;
    private int numWorkers = 1;

    public FrameGraph(AssetManager assetManager) {
        this(assetManager, new BasicRenderingQueue());
    }

    public FrameGraph(AssetManager assetManager, Executor service) {
        this(assetManager, new BasicRenderingQueue(service));
    }

    public FrameGraph(AssetManager assetManager, RenderingQueue queue) {
        this.assetManager = assetManager;
        this.queue = queue;
    }
    
    @Override
    public FrameGraphContext fetchPipelineContext(RenderManager renderManager) {
        return renderManager.getOrCreateContext(FrameGraphContext.class, rm -> new FrameGraphContext(assetManager, rm));
    }

    @Override
    public void startRenderFrame(RenderManager rm) {}

    @Override
    public void pipelineRender(RenderManager rm, FrameGraphContext pContext, ViewPort vp, float tpf) {

        // target viewport
        contextAttr.setValue(pContext);
        pContext.target(vp, tpf);

        // prepare
        for (Renderable t : this) {
            t.stage(queue);
        }
        queue.update(tpf);
        queue.prepare();

        // render
        queue.render(numWorkers);

        // reset
        queue.reset();
        rm.getRenderer().clearClipRect();
        pContext.resetAllRenderSettings();
        rendered = true;
        
    }

    @Override
    public boolean hasRenderedThisFrame() {
        return rendered;
    }

    @Override
    public void endRenderFrame(RenderManager rm) {
        rendered = false;
    }

    public <T extends Renderable> T addTask(T task) {
        add(task);
        return task;
    }

    public void setNumWorkers(int numWorkers) {
        this.numWorkers = numWorkers;
    }

    public int getNumWorkers() {
        return numWorkers;
    }

    public Socket<FrameGraphContext> getContext() {
        return contextAttr;
    }

}
