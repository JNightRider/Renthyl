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

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketArray;
import codex.renthyl.resources.tickets.TicketSelector;
import java.util.ArrayList;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 * Chooses one input group to produce all its resources as output
 * 
 * @author codex
 * @param <T>
 */
public class GroupJunction <T> extends RenderPass {
    
    public static final String OUTPUT = "Output";
    
    private final int length;
    private final ArrayList<TicketArray<T>> inputLists = new ArrayList<>();
    private TicketArray<T> output;
    private GraphSource<Integer> indexSource;
    private int nextGroupId = 0;
    
    public GroupJunction(int length) {
        this.length = length;
        this.output = new TicketArray<>(OUTPUT, this.length);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected TicketGroup createMainOutputGroup(String name) {
        return (output = new TicketArray<>(name, length));
    }
    @Override
    public void updateModule(FGRenderContext context, float tpf) {
        super.updateModule(context, tpf);
        connect(GraphSource.get(indexSource, 0, context));
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
    @SuppressWarnings("element-type-mismatch")
    public void ticketSourceChanged(TicketGroup group, ResourceTicket ticket, ResourceTicket source) {
        if (group.getNumConnectedTickets() == 0 && outputGroups.remove(group.getName(), group)) {
            inputLists.remove(group);
            group.detach();
        }
    }
    
    private void connect(int i) {
        if (i >= 0 && i < inputLists.size()) {
            output.makeInput(inputLists.get(i), TicketSelector.All, TicketSelector.All);
        } else {
            output.clearInput(TicketSelector.All);
        }
    }
    
    public void makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector, int targetGroup) {
        while (targetGroup >= inputLists.size()) {
            inputLists.add(null);
        }
        TicketArray<T> group = inputLists.get(targetGroup);
        if (group == null) {
            group = new TicketArray<>("_group" + (nextGroupId++), length);
            inputLists.set(targetGroup, group);
            addInputGroup(group);
        }
        group.makeInput(source, sourceSelector, targetSelector);
    }
    public void setIndexSource(GraphSource<Integer> index) {
        this.indexSource = index;
    }
    
    public GraphSource<Integer> getIndexSource() {
        return indexSource;
    }
    
}