/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketSelector;
import java.util.Iterator;
import java.util.Map;
import codex.renthyl.resources.tickets.TicketGroup;

/**
 *
 * @author codex
 */
public interface NewConnectable extends LayoutMember {
    
    /*******************
     * Abstract API *
     *******************/
    
    /**
     * Adds the ticket to the main input group returned by
     * {@link #getMainInputGroup()}.
     * 
     * @param <T>
     * @param ticket
     * @return added ticket
     */
    public <T> ResourceTicket<T> addInput(ResourceTicket ticket);
    
    /**
     * Adds the ticket to the main output group returned by
     * {@link #getMainOutputGroup()}.
     * 
     * @param <T>
     * @param ticket
     * @return added ticket
     */
    public <T> ResourceTicket<T> addOutput(ResourceTicket ticket);
    
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
     * Creates a new ticket with the given name and adds it to
     * the main input group.
     * 
     * @param <T>
     * @param name
     * @return created ticket
     */
    public default <T> ResourceTicket<T> addInput(String name) {
        return addInput(new ResourceTicket(name));
    }
    
    /**
     * Creates a new ticket with the given name and adds it to
     * the main output group.
     * 
     * @param <T>
     * @param name
     * @return 
     */
    public default <T> ResourceTicket<T> addOutput(String name) {
        return addOutput(new ResourceTicket(name));
    }
    
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
     * Connects the named target ticket from the main input group to the
     * named source ticket from the given Connectable's main output group.
     * 
     * @param connectable
     * @param source
     * @param target 
     */
    public default void makeInput(NewConnectable connectable, String source, String target) {
        getMainInputGroup().makeInput(connectable.getMainOutputGroup(), source, target);
    }
    
    /**
     * Connects the selected target tickets from the main input group to
     * the selected source tickets from the given Connectable's main output group.
     * 
     * @param connectable
     * @param source
     * @param target 
     */
    public default void makeInput(NewConnectable connectable, TicketSelector source, TicketSelector target) {
        getMainInputGroup().makeInput(connectable.getMainOutputGroup(), source, target);
    }
    
    /**
     * Connects the selected target tickets from the main input group to
     * the selected source tickets from the given source group.
     * 
     * @param sourceGroup
     * @param source
     * @param target 
     */
    public default void makeInput(TicketGroup sourceGroup, TicketSelector source, TicketSelector target) {
        getMainInputGroup().makeInput(sourceGroup, source, target);
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
     * Called
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
    
    public static class ConnectableIterator implements Iterator<ResourceTicket> {
        
        private final NewConnectable connectable;
        private Iterator<TicketGroup> inputGroups, outputGroups;
        private Iterator<ResourceTicket> tickets;

        public ConnectableIterator(NewConnectable connectable, boolean input, boolean output) {
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
