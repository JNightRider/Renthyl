/*
 * Copyright (c) 2025, codex
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
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Chooses one input resource to produce as its only output resource using
 * a controllable index. All other resources are ignored.
 * <p>
 * Order of the input resources is entirely determined by what order the
 * corresponding tickets are connected to this pass.
 * 
 * @author codex
 * @param <T>
 */
public class Junction <T> extends RenderPass {
    
    public static final String OUTPUT = "Output";
    
    private DynamicTicketList<T> input;
    private ResourceTicket<T> output;
    private GraphSource<Integer> source;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        output = addOutput(OUTPUT);
    }
    @Override
    protected TicketGroup createMainInputGroup(String name) {
        return (input = new DynamicTicketList<>(name));
    }
    @Override
    public void updateModule(FGRenderContext context, float tpf) {
        super.updateModule(context, tpf);
        connect(GraphSource.get(source, 0, context));
    }
    @Override
    protected void prepare(FGRenderContext context) {}
    @Override
    protected void execute(FGRenderContext context) {}
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        // this pass will never execute
        return false;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(new SavableObject(source), "source", SavableObject.NULL);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        source = SavableObject.read(in, "source", GraphSource.class);
    }
    
    private void connect(int i) {
        output.setSource(i < 0 || i >= input.size() ? null : input.select(TicketSelector.at(i)));
    }
    
    /**
     * Sets the source that determines the index.
     * 
     * @param source 
     */
    public void setIndexSource(GraphSource<Integer> source) {
        this.source = source;
    }
    /**
     * 
     * @return 
     */
    public GraphSource<Integer> getIndexSource() {
        return source;
    }
    /**
     * Gets the number of inputs attached to this pass through
     * the main input group.
     * 
     * @return 
     */
    public int getLength() {
        return input.size();
    }
    
}
