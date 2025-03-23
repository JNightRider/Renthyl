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

import codex.renthyl.resources.RenderObjectMap;
import codex.renthyl.jobs.FGJobExecutor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.pipeline.PipelineContext;
import java.util.logging.Logger;

/**
 * Manages global pipeline context for rendering with FrameGraphs.
 * 
 * @author codex
 */
public class FGPipelineContext implements PipelineContext {
    
    private static final Logger LOG = Logger.getLogger(FGPipelineContext.class.getName());
    
    private final RenderObjectMap renderObjects;
    private FGJobExecutor defaultExecutor;
    private boolean rendered = false;
    
    public FGPipelineContext(Renthyl renthyl) {
        renderObjects = new RenderObjectMap(this);
        defaultExecutor = renthyl.getBaseExecutor();
    }

    @Override
    public boolean startViewPortRender(RenderManager rm, ViewPort vp) {
        return rendered;
    }
    @Override
    public void endViewPortRender(RenderManager rm, ViewPort vp) {
        rendered = true;
    }
    @Override
    public void endContextRenderFrame(RenderManager rm) {
        renderObjects.flushMap();
        rendered = false;
    }
    
    public void setDefaultExecutor(FGJobExecutor executor) {
        defaultExecutor = (executor != null ? executor : Renthyl.getInstance().getBaseExecutor());
    }
    
    public RenderObjectMap getRenderObjects() {
        return renderObjects;
    }
    public FGJobExecutor getDefaultExecutor() {
        return defaultExecutor;
    }
    
}
