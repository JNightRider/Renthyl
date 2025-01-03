/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A ticket group that contains arbitrary tickets based on what
 * the group is currently "connected" with.
 * <p>
 * This implementation creates and destroys tickets as-needed to meet
 * connection demands and simultaneously also ensure that no tickets 
 * become stagnant and produce unwelcome errors. This means that users
 * cannot directly add any tickets to this group.
 * <p>
 * This list is most useful for passing an unknown number of resources between
 * modules, without first having to ensure that the group has the correct number
 * of tickets to handle them.
 * <p>
 * This also has the ability to "forward" its tickets to other ArbitraryTicketLists
 * (called targets). This is useful, for example, if a RenderContainer wishes to wrap
 * an ArbitraryTicketList argument for an internal module to improve API, and then 
 * forward the list to the internal module.
 * 
 * @author codex
 * @param <T>
 */
public class ArbitraryTicketList <T> extends AbstractTicketGroup<T> {
    
    private final ArrayList<ResourceTicket<T>> tickets = new ArrayList<>();
    private final ArrayList<ArbitraryTicketList<T>> targets = new ArrayList<>();
    private ArbitraryTicketList<T> source;
    
    public ArbitraryTicketList(String name) {
        super(name);
    }
    
    @Override
    public void add(ResourceTicket<T> ticket) {
        throw new UnsupportedOperationException("Tickets cannot be directly added to this group.");
    }
    @Override
    public Collection<ResourceTicket<T>> getTickets() {
        return tickets;
    }
    @Override
    public void makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        for (ResourceTicket<T> s : source) {
            if (sourceSelector.test(s)) {
                ResourceTicket<T> t = createAndInsertTicket(s);
                for (ArbitraryTicketList<T> f : targets) {
                    f.makeInput(this, TicketSelector.is(t), targetSelector);
                }
            }
        }
    }
    @Override
    public void ticketSourceChanged(ResourceTicket<T> ticket, ResourceTicket<T> source) {
        if (source == null) {
            remove(ticket);
        }
        super.ticketSourceChanged(ticket, source);
    }
    
    /**
     * Registers a target list that will actively mirror and connect to
     * each ticket in this list. Basically, this list forwards its tickets
     * to the target list.
     * <p>
     * Note: the target list will lose all tickets it currently contains, and
     * with them all previous connections.
     * 
     * @param target 
     */
    public void registerTarget(ArbitraryTicketList<T> target) {
        if (target.source != null) {
            target.source.removeTarget(target);
        }
        targets.add(target);
        target.setSource(this);
    }
    /**
     * Removes the registered target list.
     * <p>
     * The target list (if it is a target of this list) will lose all tickets
     * it currently has, and with them all connections.
     * 
     * @param target 
     */
    public void removeTarget(ArbitraryTicketList<T> target) {
        if (targets.remove(target)) {
            setSource(null);
        }
    }
    
    protected ResourceTicket<T> createAndInsertTicket(ResourceTicket<T> source) {
        ResourceTicket<T> t = insert(new ResourceTicket<>(name + ":element"));
        t.setSource(source);
        return t;
    }
    protected void setSource(ArbitraryTicketList<T> source) {
        if (this.source != null && this.source != source) {
            clear();
        }
        this.source = source;
        if (this.source != null) {
            for (ResourceTicket<T> t : this.source.getTickets()) {
                createAndInsertTicket(t);
            }
        }
    }
    
}
