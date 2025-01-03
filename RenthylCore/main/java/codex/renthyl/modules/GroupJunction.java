/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
 *
 * @author codex
 * @param <T>
 */
public class GroupJunction <T> extends RenderPass {
    
    public static final String OUTPUT = "Output";
    
    private final int length;
    private final ArrayList<TicketArray<T>> inputLists = new ArrayList<>();
    private final TicketArray<T> output;
    private GraphSource<Integer> indexSource;
    private int nextGroupId = 0;
    
    public GroupJunction(int length) {
        this.length = length;
        this.output = new TicketArray<>(OUTPUT, this.length);
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        addOutputGroup(output);
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
    
    public TicketGroup<T> getOutputGroup() {
        return output;
    }
    public GraphSource<Integer> getIndexSource() {
        return indexSource;
    }
    
}