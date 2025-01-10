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

import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import java.util.Iterator;
import java.util.Map;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.resources.tickets.TicketSignature;

/**
 * An object that can be connected to other Connectables by
 * {@link TicketGroup TicketGroups} containing {@link ResourceTicket ResourceTickets}.
 * 
 * @author codex
 */
public interface Connectable extends LayoutMember {
    
    /****************
     * Abstract API *
     ****************/
    
    /**
     * Adds the ticket to the main input group returned by
     * {@link #getMainInputGroup()}.
     * 
     * @param <T>
     * @param name
     * @return added ticket
     */
    public ResourceTicket addInput(String name);
    
    /**
     * Adds the ticket to the main output group returned by
     * {@link #getMainOutputGroup()}.
     * 
     * @param <T>
     * @param name
     * @return added ticket
     */
    public ResourceTicket addOutput(String name);
    
    /**
     * Adds and attaches the group as input.
     * 
     * @param <T>
     * @param <R>
     * @param group
     * @return added group
     */
    public <T, R extends TicketGroup<T>> R addInputGroup(R group);
    
    /**
     * Adds and attaches the group as output.
     * 
     * @param <T>
     * @param <R>
     * @param group
     * @return added group
     */
    public <T, R extends TicketGroup<T>> R addOutputGroup(R group);
    
    /**
     * Returns all added input groups.
     * 
     * @return 
     */
    public Map<String, TicketGroup> getInputGroups();
    
    /**
     * Returns all added output groups.
     * 
     * @return 
     */
    public Map<String, TicketGroup> getOutputGroups();
    
    /**
     * Gets the main input group which individual tickets are added to.
     * 
     * @return 
     */
    public TicketGroup<Object> getMainInputGroup();
    
    /**
     * Gets the main output group which individual tickets are added to.
     * 
     * @return 
     */
    public TicketGroup<Object> getMainOutputGroup();
    
    /*******************
     * Implemented API *
     *******************/
    
    /**
     * Gets the named input group.
     * 
     * @param name
     * @return named group, or null
     */
    public default TicketGroup getInputGroup(String name) {
        return getInputGroups().get(name);
    }
    
    /**
     * Gets the named input group of the specified type.
     * 
     * @param <T>
     * @param type
     * @param name
     * @return 
     */
    public default <T extends TicketGroup> T getInputGroup(Class<T> type, String name) {
        return (T)getInputGroups().get(name);
    }
    
    /**
     * Gets the named output group.
     * 
     * @param name
     * @return named group, or null
     */
    public default TicketGroup getOutputGroup(String name) {
        return getOutputGroups().get(name);
    }
    
    /**
     * Gets the named output group of the specified type.
     * 
     * @param <T>
     * @param type
     * @param name
     * @return 
     */
    public default <T extends TicketGroup> T getOutputGroup(Class<T> type, String name) {
        return (T)getOutputGroups().get(name);
    }
    
    /**
     * Gets the named group.
     * 
     * @param name
     * @param input true if the group is an input group
     * @return 
     */
    public default TicketGroup getGroup(String name, boolean input) {
        if (input) {
            return getInputGroup(name);
        } else {
            return getOutputGroup(name);
        }
    }
    
    /**
     * Gets the named group of the specified type.
     * 
     * @param <T>
     * @param type
     * @param name
     * @param input true if the group is an input group
     * @return 
     */
    public default <T extends TicketGroup> T getGroup(Class<T> type, String name, boolean input) {
        if (input) {
            return getInputGroup(type, name);
        } else {
            return getOutputGroup(type, name);
        }
    }
    
    /**
     * Connects the named target ticket from the main input group to the
     * named source ticket from the given Connectable's main output group.
     * 
     * @param source
     * @param sourceSelector
     * @param targetSelector 
     * @return number of connections made
     */
    public default int makeInput(Connectable source, String sourceSelector, String targetSelector) {
        return getMainInputGroup().makeInput(source.getMainOutputGroup(), sourceSelector, targetSelector);
    }
    
    /**
     * Connects the selected target tickets from the main input group to
     * the selected source tickets from the given source group.
     * 
     * @param source
     * @param sourceSelector
     * @param targetSelector 
     */
    public default void makeInput(TicketGroup source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        getMainInputGroup().makeInput(source, sourceSelector, targetSelector);
    }
    
    /**
     * 
     * @param source
     * @param sourceSig
     * @param targetSig 
     */
    public default void makeInput(Connectable source, TicketSignature sourceSig, TicketSignature targetSig) {
        targetSig.getGroupOf(this).makeInput(sourceSig.getGroupOf(source), sourceSig, targetSig);
    }
    
    /**
     * Creates an iterator that iterates over tickets contained in ticket groups
     * contained in this Connectable.
     * 
     * @param input true to iterate over input tickets
     * @param output true to iterate over output tickets
     * @return 
     */
    public default Iterator<ResourceTicket> ticketIterator(boolean input, boolean output) {
        return new ConnectableIterator(this, input, output);
    }
    
    /**
     * Called when the source of a ticket within this Connectable changes.
     * 
     * @param group
     * @param ticket
     * @param source 
     */
    public default void ticketSourceChanged(TicketGroup group, ResourceTicket ticket, ResourceTicket source) {
        
    }
    
    /***********
     * Classes *
     ***********/
    
    /**
     * Iterator for iterating over each ticket within a Connectable.
     */
    public static class ConnectableIterator implements Iterator<ResourceTicket> {
        
        private final Connectable connectable;
        private Iterator<TicketGroup> inputGroups, outputGroups;
        private Iterator<ResourceTicket> tickets;

        public ConnectableIterator(Connectable connectable, boolean input, boolean output) {
            this.connectable = connectable;
            if (input) {
                inputGroups = this.connectable.getInputGroups().values().iterator();
            }
            if (output) {
                outputGroups = this.connectable.getOutputGroups().values().iterator();
            }
        }
        
        @Override
        public boolean hasNext() {
            return (inputGroups = groupHasNext(inputGroups)) != null
                    || (outputGroups = groupHasNext(outputGroups)) != null;
        }

        @Override
        public ResourceTicket next() {
            return tickets.next();
        }
        
        private Iterator<TicketGroup> groupHasNext(Iterator<TicketGroup> group) {
            // find the next non-empty ticket iterator
            while (group != null && group.hasNext() && (tickets == null || !tickets.hasNext())) {
                tickets = group.next().getTickets().iterator();
            }
            // always returns null (does not have next) if the group iterator is null
            return ((tickets != null && tickets.hasNext()) ? group : null);
        }
        
    }
    
}
