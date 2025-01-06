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

import codex.renthyl.export.NamedTicketIndex;
import codex.renthyl.export.TicketIndex;
import java.util.Iterator;
import codex.renthyl.modules.Connectable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Basic implementation of a TicketGroup.
 * 
 * @author codex
 * @param <T>
 */
public abstract class AbstractTicketGroup <T> implements TicketGroup<T> {
    
    protected final String name;
    protected Connectable owner;
    private int connectedTickets = 0;
    private final ArrayList<ResourceTicket<T>> tempTargets = new ArrayList<>();

    public AbstractTicketGroup(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    @Override
    public void attach(Connectable owner) {
        if (this.owner != null) {
            throw new IllegalStateException("Group is already attached to a Connectable.");
        }
        this.owner = owner;
        this.owner.setLayoutUpdateNeeded();
    }
    @Override
    public void detach() {
        if (owner == null) {
            throw new IllegalStateException("Group is not attached to a Connectable.");
        }
        clear();
        owner = null;
    }
    @Override
    public void add(ResourceTicket<T> ticket) {
        append(ticket);
    }
    @Override
    @SuppressWarnings("null")
    public void makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        targetSelector.selectFromOrElse(this, tempTargets, null);
        int j = 0;
        for (ResourceTicket<T> s : source) {
            if (sourceSelector.select(s, j)) {
                for (int i = 0; i < tempTargets.size(); i++) {
                    ResourceTicket<T> t = tempTargets.get(i);
                    if (t != null && sourceSelector.select(s, t, j) && targetSelector.select(t, s, i)) {
                        t.setSource(s);
                        tempTargets.set(i, null);
                    }
                }
            }
            j++;
        }
        tempTargets.clear();
    }
    @Override
    public void ticketSourceChanged(ResourceTicket<T> ticket, ResourceTicket<T> source) {
        if (ticket.getGroup() == this) {
            if (ticket.getSource() == null && source != null) {
                connectedTickets++;
            } else if (ticket.getSource() != null && source == null) {
                connectedTickets--;
            }
        }
        TicketGroup.super.ticketSourceChanged(ticket, source);
        owner.ticketSourceChanged(this, ticket, source);
    }
    @Override
    public void setLayoutUpdateNeeded() {
        if (owner != null) {
            owner.setLayoutUpdateNeeded();
        }
    }
    @Override
    public Iterator<ResourceTicket<T>> iterator() {
        return getTickets().iterator();
    }
    @Override
    public int getNumConnectedTickets() {
        return connectedTickets;
    }
    @Override
    public void applySavedConnections(Map<Integer, Connectable> registry, Collection<TicketIndex> indices) {
        for (TicketIndex i : indices) {
            Objects.requireNonNull(i.getSource(), "Connection source not defined.");
            TicketSelector target = i.createSelector();
            TicketSelector source = i.getSource().createSelector();
            makeInput(i.getSource().getGroup(registry), source, target);
        }
    }
    @Override
    public void generateExportIndices(Consumer<TicketIndex> setup) {
        for (ResourceTicket<T> t : getTickets()) {
            NamedTicketIndex i = new NamedTicketIndex();
            setup.accept(i);
            t.setExportIndex(i);
        }
    }
    
    /**
     * Appends the ticket to the end of the {@link #getTickets() ticket
     * collection}.
     * 
     * @param ticket
     * @return 
     */
    protected ResourceTicket<T> append(ResourceTicket ticket) {
        getTickets().add(ticket);
        ticket.setGroup(this);
        return ticket;
    }
    
    /**
     * Removes the ticket from the {@link #getTickets() ticket collection}.
     * <p>
     * The ticket's source, targets, and group are cleared if the ticket
     * was part of the ticket collection.
     * 
     * @param ticket
     * @return 
     */
    protected boolean remove(ResourceTicket<T> ticket) {
        if (getTickets().remove(ticket)) {
            ticket.setSource(null);
            ticket.clearAllTargets();
            ticket.setGroup(null);
            return true;
        }
        return false;
    }
    
    /**
     * Clears all tickets from the {@link #getTickets() ticket collection}.
     * <p>
     * Each ticket's source, targets, and group are cleared.
     */
    protected void clear() {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
            t.clearAllTargets();
            t.setGroup(null);
        }
        getTickets().clear();
    }
    
}
