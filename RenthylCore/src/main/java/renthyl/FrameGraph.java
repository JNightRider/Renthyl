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
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.renderer.ViewPort;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Rendering pipeline which renders from a render graph of {@link Renderable renderable tasks} connected by
 * {@link Socket sockets}.
 *
 * <p>Before each render, the render graph is staged (flattened) into a {@link RenderingQueue}, which is then
 * responsible for performing each pipeline step. The staging process ensures that only necessary tasks
 * get processed further and rendered. Tasks do not need to be directly added to a FrameGraph in order
 * to participate in rendering. The staging process automatically stages tasks that other tasks depend on
 * for rendering.</p>
 *
 * <p>By default, FrameGraph uses a {@link BasicRenderingQueue}, which is not publically exposed. The queue used
 * can be set using a {@link FrameGraph#FrameGraph(RenderingQueue) constructor}.</p>
 *
 * <p>Note that FrameGraph only acts as a thin interface between the application loop and the
 * render graph. In fact, a render graph may be rendered without involving a FrameGraph by directly
 * interfacing with a RenderingQueue.</p>
 *
 * <pre><code>
 * RenderingQueue queue = ...
 * GlobalAttributes globals = ...
 * Collection&lt;Renderable&gt; tasksToRender = ...
 * for (Renderable r : tasksToRender) {
 *     r.stage(globals, queue);
 * }
 * queue.update(tpf);
 * queue.prepare();
 * queue.render();
 * queue.reset();
 * </code></pre>
 *
 * <p>This can be shortened using {@link RenderingQueue#pipeline(GlobalAttributes, Iterable, float)
 * RenderingQueue.pipeline}.</p>
 *
 * <pre><code>
 * queue.pipeline(globals, tasksToRender, tpf);
 * </code></pre>
 *
 * @author codex
 */
public class FrameGraph extends ArrayList<Renderable> {

    private final RenderingQueue queue;

    /**
     * Creates a FrameGraph using a {@link BasicRenderingQueue} without an {@link Executor}.
     */
    public FrameGraph() {
        this(new BasicRenderingQueue());
    }

    /**
     * Creates a FrameGraph using a {@link BasicRenderingQueue} with an executor for multithreading.
     *
     * @param service
     */
    public FrameGraph(Executor service) {
        this(new BasicRenderingQueue(service));
    }

    /**
     * Creates a FrameGraph using the rendering queue.
     *
     * @param queue
     */
    public FrameGraph(RenderingQueue queue) {
        this.queue = queue;
    }

    /**
     * Renders the FrameGraph.
     *
     * @param globals global attributes
     * @param tpf estimated time per frame
     */
    public void render(GlobalAttributes globals, float tpf) {
        queue.pipeline(globals, this, tpf);
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

}
