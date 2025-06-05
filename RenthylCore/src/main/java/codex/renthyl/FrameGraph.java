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

import codex.renthyl.render.ContextRenderer;
import codex.renthyl.render.queue.BasicRenderingQueue;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.attributes.SynchronizedAttribute;
import codex.renthyl.util.Stopwatch;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.renderer.ViewPort;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Rendering pipeline which renders from a graph of {@link Renderable renderable tasks} connected by
 * {@link Socket sockets}.
 *
 * <p>The FrameGraph pipeline is divided into several distinct steps during rendering. First, all tasks,
 * starting from the tasks directly attached to the FrameGraph, are recursively staged. That is, they are
 * added to a {@link RenderingQueue} for further processing <em>and</em> any other tasks they depend on.
 * This means that tasks not directly attached to the FrameGraph may still be staged for rendering by the
 * FrameGraph</p>
 *
 * <p>The next rendering step is updating, during which all staged tasks (and the sockets they contain)
 * are updated. Then the preparation step occurs, where tasks reference the resources they intend on
 * using.</p>
 *
 * <p>Finally, all staged tasks are rendered. The precise order in which tasks are rendered depends on
 * precise the {@link RenderingQueue} implementation. However, if only a single thread is used, tasks are
 * rendered more or less in the order they were staged.</p>
 *
 * <p>After rendering, all staged tasks (and the sockets they contain) are reset in preparation for the next
 * render frame.</p>
 */
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
            t.stage(pContext.getGlobals(), queue);
        }
        queue.update(tpf);
        queue.prepare();

        // render
        queue.render(pContext.getRenderManager().getProfiler(), numWorkers);

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

    /**
     * Adds a task to this FrameGraph and returns it.
     *
     * <p>Note that adding a task to the FrameGraph multiple times should have
     * no visible impact.</p>
     *
     * @param task task to add
     * @return
     * @param <T>
     */
    public <T extends Renderable> T addTask(T task) {
        add(task);
        return task;
    }

    /**
     * Sets the number of workers used during render.
     *
     * @param numWorkers number of workers (must be positive)
     */
    public void setNumWorkers(int numWorkers) {
        this.numWorkers = numWorkers;
    }

    /**
     * Gets the number of workers used during render.
     *
     * @return
     */
    public int getNumWorkers() {
        return numWorkers;
    }

    /**
     * Gets the socket that shares the {@link FrameGraphContext} instance.
     *
     * @return
     */
    public Socket<FrameGraphContext> getContext() {
        return contextAttr;
    }

}
