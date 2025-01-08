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
package codex.renthyl.modules;

import codex.boost.export.SavableObject;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 *
 * @author codex
 * @param <T>
 */
public class RenderLoop <T extends AbstractRenderModule> extends RenderContainer<T> {
    
    private LoopBuilder<T> builder;
    private GraphSource<Integer> iterationSource;
    private int currentIterations = 0;
    
    /**
     * Serialization only.
     */
    public RenderLoop() {}
    
    public RenderLoop(LoopBuilder<T> builder) {
        this.builder = builder;
    }
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        super.initModule(frameGraph);
        builder.initLoop(this);
    }
    @Override
    public void updateModule(FGRenderContext context, float tpf) {
        super.updateModule(context, tpf);
        int iterations = getNumIterations(context);
        if (iterations <= 0) {
            throw new IllegalArgumentException("Cannot perform less than one loop iteration.");
        }
        while (queue.size() > iterations) {
            builder.createIteration(this);
        }
        while (queue.size() < iterations) {
            builder.removeIteration(this, queue.get(queue.size()-1), queue.size()-1);
        }
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(currentIterations, "currentIterations", 0);
        out.write(new SavableObject(builder), "connector", SavableObject.NULL);
        out.write(new SavableObject(iterationSource), "iterationSource", SavableObject.NULL);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        currentIterations = in.readInt("currentIterations", 0);
        builder = SavableObject.read(in, "connector", LoopBuilder.class);
        iterationSource = SavableObject.read(in, "iterationSource", GraphSource.class);
    }
    
    private int getNumIterations(FGRenderContext context) {
        if (iterationSource != null) {
            int i = iterationSource.getGraphValue(frameGraph, context.getViewPort());
            if (i <= 0 && i != currentIterations) {
                frameGraph.setLayoutUpdateNeeded();
                currentIterations = i;
            }
        }
        if (currentIterations <= 0) {
            currentIterations = 1;
        }
        return currentIterations;
    }
    
    /**
     * Sets the connection manager responsible for creating and
     * connecting iteration modules.
     * 
     * @param builder connection manager (not null)
     */
    public void setBuilder(LoopBuilder<T> builder) {
        assert builder != null;
        this.builder = builder;
    }
    /**
     * Sets the graph source that dictates the number of iterations
     * that will occur.
     * <p>
     * At least one iteration must exist, but if the source returns less
     * than or equal to zero, the current number of iterations will be used
     * (or one).
     * <p>
     * If the iteration source is null, the current number of iterations will
     * be used (or one).
     * 
     * @param source iteration source (may be null)
     */
    public final void setIterationSource(GraphSource<Integer> source) {
        this.iterationSource = source;
    }
    
    public T getIterationModule(int i) {
        if (i < 0 || i >= queue.size()-2) {
            throw new IndexOutOfBoundsException("Index "+i+" out of bounds for length "+(queue.size()-2));
        }
        return (T)queue.get(i+1);
    }
    public int getCurrentNumIterations() {
        return currentIterations;
    }
    
}
