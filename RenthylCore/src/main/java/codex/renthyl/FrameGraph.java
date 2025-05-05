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

import codex.renthyl.newresources.BasicRenderingQueue;
import codex.renthyl.newresources.Renderable;
import codex.renthyl.newresources.RenderingQueue;
import codex.renthyl.resources.ResourceList;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderThread;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.pipeline.RenderPipeline;
import com.jme3.renderer.ViewPort;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import codex.renthyl.modules.RenderPass;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * <p>
 * Rendering is a complicated task, involving many parameters and resources. The framegraph
 * aims to simplify rendering from the user's perspective, and limit the creation, binding,
 * and destruction of resources wherever possible.
 * <p>
 * Passes are expected to declare or reference beforehand the resources they plan on using
 * during execution. These declarations are stored in a ResourceList as
 * {@link codex.renthyl.resources.ResourceView ResourceViews}, which are used to determine
 * how resources (such as textures) should be allocated and which passes should be culled.
 * <p>
 * During rendering (called execution), passes then expected ask the ResourceList
 * for the resource the declared or referenced earlier. If the resource does not already
 * exist (is virtual), the manager will either create a new resource or allocate an existing
 * unused resource that qualifies based on the description provided when declared.
 * <p>
 * To understand how resources are managed, see {@link ResourceList} documentation.
 * <p>
 * FrameGraph execution generally occurs in four steps:
 * <ol>
 *  <li><strong>Preparation.</strong> Passes declare, reserve, and reference resources
 * during this step.</li>
 *  <li><strong>Culling.</strong> The resource manager determines which resources and
 * passes are unused, and culls them. This can often save loads of resources, as many
 * passes may not used for large parts of the application.</li>
 *  <li><strong>Execution.</strong> Passes that were not culled acquire the resources
 * they need, and perform rendering operations. All passes are expected to release
 * all resources they declared or referenced in the first step, however, this is done
 * automatically by {@link RenderPass}.</li>
 *  <li><strong>Reset.</strong> Passes perform whatever post-rendering cleanup is necessary.</li>
 * </ol>
 * Passes are organized in a scenegraph-like structure, where {@link RenderContainer RenderContainers}
 * act as nodes which can contain any number of other modules (such as {@link RenderPass RenderPasses}).
 * Modules are executed <em>depth-first</em>, meaning a RenderContainer executes all
 * its child modules before the next module in the original queue.
 * <pre>
 *        E F G
 *        -----
 *          |
 * -&gt; A B C D H I -&gt;</pre>
 * <em>Modules are executed alphabetically starting with {@code A} and ending with
 * {@code I}. Module {@code D} is a RenderContainer containing modules {@code E},
 * {@code F}, and {@code G}.</em>
 * <p>
 * The reason for this setup, rather than a direct linear queue, is so that specific
 * groups of modules can be organized independently of other module groups. An
 * example of this is a {@code codex.renthyl.modules.RenderLoop RenderLoop}, which
 * programmatically generates modules within itself to simulate looping.
 * <p>
 * This setup also gracefully allows for multithreading via {@link RenderThread RenderThreads}.
 * Any descendent of a RenderThread module is executed on the same worker thread as the
 * RenderThread. RenderThreads can be used anywhere within the tree, and multiple
 * RenderThreads can use the same worker thread.
 * <p>
 * Communication is allowed between game logic and FrameGraph internals through
 * {@link GraphSource} and {@link codex.renthyl.client.GraphTarget GraphTarget}.
 * The FrameGraph facilitates this communication by providing a settings map, which
 * acts as a mediator between game logic and internals.
 * 
 * @author codex
 */
public class FrameGraph extends ArrayList<Renderable> implements RenderPipeline<FGPipelineContext> {

    public static final Class<FGPipelineContext> CONTEXT_TYPE = FGPipelineContext.class;

    private final FGRenderContext context;
    private final RenderingQueue queue;
    private boolean rendered = false;

    public FrameGraph(AssetManager assetManager) {
        this(assetManager, new BasicRenderingQueue(Executors.newCachedThreadPool()));
    }

    public FrameGraph(AssetManager assetManager, RenderingQueue queue) {
        this.context = new FGRenderContext(assetManager);
        this.queue = queue;
    }
    
    @Override
    public FGPipelineContext fetchPipelineContext(RenderManager rm) {
        return rm.getContext(CONTEXT_TYPE);
    }

    @Override
    public void startRenderFrame(RenderManager rm) {
        // require that renthyl be initialized
        Renthyl.requireInitialized();
    }

    @Override
    public void pipelineRender(RenderManager rm, FGPipelineContext pContext, ViewPort vp, float tpf) {

        // target viewport
        rm.applyViewPort(vp);
        context.target(rm, pContext, vp, tpf);

        // prepare
        for (Renderable t : this) {
            t.queue(queue);
        }
        for (Renderable t : queue) {
            t.update(tpf);
        }
        queue.forEach(Renderable::prepare);

        // render
        final int numRenderWorkers = 1;
        queue.render(numRenderWorkers, context);

        // reset
        queue.forEach(Renderable::reset);
        queue.flush();
        rm.getRenderer().clearClipRect();
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
    
}
