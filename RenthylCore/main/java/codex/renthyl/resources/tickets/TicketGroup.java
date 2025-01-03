/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.modules.LayoutMember;
import codex.renthyl.modules.NewConnectable;
import codex.renthyl.resources.ResourceList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 * @author codex
 * @param <T>
 */
public interface TicketGroup <T> extends LayoutMember, Iterable<ResourceTicket<T>> {
    
    /****************
     * Abstract API *
     ****************/
    
    /**
     * Updates this group.
     */
    public void update();
    
    /**
     * Returns the name of this group.
     * 
     * @return 
     */
    public String getName();
    
    /**
     * Assigns this group to the owner.
     * <p>
     * If this group is already assigned to an owner,
     * an exception will be thrown.
     * 
     * @param owner 
     */
    public void attach(NewConnectable owner);
    
    /**
     * Cleans up the group.
     * <p>
     * Called if the group is removed from its owner or the owner is
     * removed from the framegraph.
     */
    public void detach();
    
    /**
     * Adds the given ticket to this group.
     * 
     * @param ticket 
     */
    public void add(ResourceTicket<T> ticket);
    
    /**
     * Gets all tickets in this group.
     * 
     * @return 
     */
    public Collection<ResourceTicket<T>> getTickets();
    
    /**
     * Connects each target ticket accepted by the target selector to a
     * corresponding source ticket accepted by the source selector.
     * 
     * @param source
     * @param sourceSelector
     * @param targetSelector 
     */
    public void makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector);
    
    /**
     * Gets the number of tickets in the group that have a
     * non-null source.
     * 
     * @return 
     */
    public int getNumConnectedTickets();
    
    /*******************
     * Implemented API *
     *******************/
    
    /**
     * Gets the first ticket accepted by the selector.
     * 
     * @param selector
     * @return 
     */
    public default ResourceTicket<T> select(TicketSelector selector) {
        return selector.selectFrom(getTickets());
    }
    
    /**
     * Adds each selected ticket in this group to {@code collection}.
     * 
     * @param <R>
     * @param selector
     * @param collection
     * @return collection containing selected tickets
     */
    public default <R extends Collection<ResourceTicket<T>>> R select(TicketSelector selector, R collection) {
        return selector.selectFrom(getTickets(), collection);
    }
    
    /**
     * Returns the size of this group.
     * 
     * @return 
     */
    public default int size() {
        return getTickets().size();
    }
    
    /**
     * Called when the source of a ticket in this group is changing.
     * 
     * @param ticket ticket whose source is changing
     * @param source the new source that will be assigned to the ticket
     */
    public default void ticketSourceChanged(ResourceTicket<T> ticket, ResourceTicket<T> source) {
        setLayoutUpdateNeeded();
    }
    
    /**
     * Disconnects all tickets in this group from all target and source tickets.
     */
    public default void disconnect() {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
            t.clearAllTargets();
        }
    }
    
    /**
     * Connects the named target ticket from this group to the named source
     * ticket from the source group.
     * 
     * @param source
     * @param sourceName
     * @param targetName 
     */
    public default void makeInput(TicketGroup<T> source, String sourceName, String targetName) {
        makeInput(source, TicketSelector.name(sourceName), TicketSelector.name(targetName));
    }
    
    /**
     * Connects all named target tickets from this group to the corresponding
     * named source tickets from the source group.
     * <p>
     * The source names and target names are intertwined within the same array
     * {@code names}. Every other name is expected to be a source name, with the
     * corresponding target name being directly after.
     * 
     * @param source
     * @param names
     * @throws IllegalArgumentException if an odd number of names is passed
     */
    public default void makeAllInput(TicketGroup<T> source, String... names) {
        if ((names.length & 1) == 1) {
            throw new IllegalArgumentException("An even number of names is required (found " + names.length + ").");
        }
        for (int i = 0; i < names.length; i += 2) {
            makeInput(source, TicketSelector.name(names[i]), TicketSelector.name(names[i + 1]));
        }
    }
    
    /**
     * 
     * @param selector 
     */
    public default void clearInput(TicketSelector selector) {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
        }
    }
    
    /**
     * Releases all resources associated with all tickets in this group.
     * 
     * @param resources 
     */
    public default void releaseAll(ResourceList resources) {
        for (ResourceTicket<T> t : getTickets()) {
            resources.releaseOptional(t);
        }
    }
    
}
