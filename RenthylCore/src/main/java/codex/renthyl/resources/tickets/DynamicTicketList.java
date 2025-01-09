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
package codex.renthyl.resources.tickets;

import codex.renthyl.export.TicketIndex;
import codex.renthyl.modules.Connectable;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A ticket group that contains arbitrary tickets based on what
 * the group is currently "connected" with.
 * <p>
 * This implementation creates and destroys tickets as-needed to meet
 * connection demands and simultaneously ensure that no tickets become
 * stagnant and produce unwelcome errors. This means that each ticket
 * within this group is guaranteed to be connected with something.
 * <p>
 * This list is most useful for passing an unknown number of resources between
 * modules, without first having to ensure that the group has the correct number
 * of tickets to handle them.
 * <p>
 * This also has the ability to "forward" its tickets to other DynamicTicketLists.
 * This is useful, for example, if a RenderContainer wishes to wrap an
 * DynamicTicketList argument for an internal module (to improve API), and then 
 * forward the list to the internal module.
 * 
 * @author codex
 * @param <T>
 */
public class DynamicTicketList <T> extends AbstractTicketGroup<T> {
    
    private final ArrayList<ResourceTicket<T>> tickets = new ArrayList<>();
    private final ArrayList<DynamicTicketList<T>> targets = new ArrayList<>();
    private int maxElements = -1;
    private long nextTicketId = 0;
    
    public DynamicTicketList(String name) {
        super(name);
    }
    
    @Override
    public void detach() {
        super.detach();
        targets.clear();
    }
    @Override
    public ResourceTicket<T> add(String name) {
        throw new UnsupportedOperationException("Tickets cannot be directly added to this group.");
    }
    @Override
    public Collection<ResourceTicket<T>> getTickets() {
        return tickets;
    }
    @Override
    public int makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        int i = 0;
        int connections = 0;
        for (ResourceTicket<T> s : source) {
            if (maxElements >= 0 && tickets.size() >= maxElements) {
                break;
            }
            if (sourceSelector.select(s, i)) {
                ResourceTicket t = createTicket();
                if (sourceSelector.select(s, t, i) && targetSelector.select(t, s, tickets.size())) {
                    append(t).setSource(s);
                    connections++;
                    TicketSelector selector = TicketSelector.is(t);
                    for (DynamicTicketList<T> f : targets) {
                        f.makeInput(this, selector, TicketSelector.All);
                    }
                }
            }
            i++;
        }
        return connections;
    }
    @Override
    public void ticketSourceChanged(ResourceTicket<T> ticket, ResourceTicket<T> source) {
        if (source == null) {
            remove(ticket);
        }
        super.ticketSourceChanged(ticket, source);
    }
    @Override
    public void applySavedConnections(Map<Integer, Connectable> registry, Collection<TicketIndex> indices) {
        indices.stream().map(i -> i.cast(ListTicketIndex.class)).sorted((o1, o2) -> {
            if (o1.sortIndex > o2.sortIndex) return 1;
            else if (o1.sortIndex < o2.sortIndex) return -1;
            else return 0;
        }).forEach(i -> {
            makeInput(i.getGroup(registry), i.getSource().createSelector(), TicketSelector.All);
        });
    }
    @Override
    public void generateExportIndices(Consumer<TicketIndex> setup) {
        int sort = 0;
        for (ResourceTicket<T> t : tickets) {
            ListTicketIndex i = new ListTicketIndex(sort++);
            setup.accept(i);
            t.setExportIndex(i);
        }
    }
    
    /**
     * Registers a target list that will actively mirror and connect to
     * each ticket in this list. Basically, this list forwards its tickets
     * to the target list.
     * 
     * @param target 
     */
    public void registerTargetList(DynamicTicketList<T> target) {
        targets.add(target);
        target.makeInput(this, TicketSelector.All, TicketSelector.All);
    }
    
    /**
     * Removes the registered target list.
     * <p>
     * Inputs from this list to the target list are cleared.
     * 
     * @param target 
     * @return true if the target list was removed
     */
    public boolean removeTargetList(DynamicTicketList<T> target) {
        if (targets.remove(target)) {
            target.clearInput((t, o, i) -> t.hasSource() && t.getSource().getGroup() == this);
            return true;
        }
        return false;
    }
    
    /**
     * Sets the maximum tickets that can be in this group at once before
     * further additions are rejected.
     * <p>
     * Tickets are not automatically removed if {@code maxElements}
     * is set below the current number of tickets, however, no more
     * tickets can be added. If {@code maxElements} is negative, the
     * number of tickets is unbounded.
     * <p>
     * To trim the number of tickets to the current maximum, use
     * {@link #trimToMaxElements()}.
     * <p>
     * default=-1 (no bound)
     * 
     * @param maxElements 
     */
    public void setMaxElements(int maxElements) {
        this.maxElements = maxElements;
    }
    
    /**
     * Gets the maximum number of tickets that can be added to this group.
     * 
     * @return
     * @see #setMaxElements(int)
     */
    public int getMaxElements() {
        return maxElements;
    }
    
    /**
     * Removes tickets from the end of this group's collection until
     * the number of tickets is less that or equal to the current
     * maximum elements.
     * 
     * @see #setMaxElements(int)
     */
    public void trimToMaxElements() {
        if (maxElements >= 0) {
            for (int i = tickets.size()-1; i >= maxElements; i--) {
                tickets.get(i).setSource(null);
            }
        }
    }
    
    protected ResourceTicket<T> createAndAppendTicket(ResourceTicket<T> source) {
        ResourceTicket<T> t = append(createTicket());
        t.setSource(source);
        return t;
    }
    protected ResourceTicket<T> createTicket() {
        return new ResourceTicket<>(this, name + ":" + nextTicketId++);
    }
    
    public static class ListTicketIndex extends TicketIndex {
        
        public int sortIndex = 0;
        
        public ListTicketIndex() {}
        public ListTicketIndex(int sortIndex) {
            this.sortIndex = sortIndex;
        }
        
        @Override
        protected void write(OutputCapsule out) throws IOException {
            out.write(sortIndex, "sortIndex", 0);
        }
        @Override
        protected void read(InputCapsule in) throws IOException {
            sortIndex = in.readInt("sortIndex", 0);
        }
        @Override
        public TicketSelector createSelector() {
            return TicketSelector.First;
        }
        
    }
    
}
