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
package codex.renthyl.modules.cache;

import codex.boost.export.SavableObject;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.definitions.SimpleDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Outputs a resource from the cache.
 * 
 * @author codex
 * @param <T>
 * @see CacheWrite
 */
public class CacheRead <T> extends RenderPass {
    
    public static final String OUTPUT = "Output";
    
    private GraphSource<String> keySource;
    private ResourceTicket output;
    private SimpleDef<T> def;
    
    public CacheRead(Class<T> type) {
        this(type, (GraphSource)null);
    }
    public CacheRead(Class<T> type, String key) {
        this(type, GraphSource.value(key));
    }
    public CacheRead(Class<T> type, GraphSource<String> keySource) {
        this.keySource = keySource;
        this.def = new SimpleDef<>(type);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        output = addOutput(OUTPUT);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(def, output);
    }
    @Override
    protected void execute(FGRenderContext context) {
        String key = keySource.getGraphValue(frameGraph, context.getViewPort());
        if (key != null) {
            resources.acquireCached(output, key);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected void write(OutputCapsule out) throws IOException {
        out.write(new SavableObject(keySource), "keySource", SavableObject.NULL);
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        keySource = SavableObject.read(in, "keySource", GraphSource.class);
    }
    
    /**
     * Sets the source of the cache key to use.
     * 
     * @param keySource 
     */
    public void setKeySource(GraphSource<String> keySource) {
        this.keySource = keySource;
    }
    
    /**
     * Gets the source of the cache key to use.
     * 
     * @return 
     */
    public GraphSource<String> getKeySource() {
        return keySource;
    }
    
}
