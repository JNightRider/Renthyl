/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.modules.NewConnectable;
import java.util.Iterator;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class AbstractTicketGroup <T> implements TicketGroup<T> {
    
    protected final String name;
    protected NewConnectable owner;
    private int connectedTickets = 0;

    public AbstractTicketGroup(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    @Override
    public void update() {}
    @Override
    public void attach(NewConnectable owner) {
        if (owner != null) {
            throw new IllegalStateException("Collection is already attached to a Connectable.");
        }
        this.owner = owner;
        this.owner.setLayoutUpdateNeeded();
    }
    @Override
    public void detach() {
        if (owner == null) {
            throw new IllegalStateException("Collection is not attached to a Connectable.");
        }
        clear();
        owner = null;
    }
    @Override
    public void add(ResourceTicket<T> ticket) {
        insert(ticket);
    }
    @Override
    @SuppressWarnings("null")
    public void makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector) {
        // Implementation note:
        // This is not implemented in the interface, because it uses a private helper method
        // that I'd rather not have exposed to the public API.
        Iterator<ResourceTicket<T>> sourceIt = source.iterator();
        Iterator<ResourceTicket<T>> targetIt = iterator();
        // I don't think I'll ever connect more than 1000 tickets, and this may save me later.
        for (int i = 0;; i++) {
            if (i >= 1000) {
                throw new IndexOutOfBoundsException("An attempt was made at connecting more than 1000 tickets: aborting.");
            }
            // find an acceptable source ticket to use
            ResourceTicket<T> s = findNextAcceptableTicket(sourceIt, sourceSelector);
            if (s == null) {
                break;
            }
            // find a corresponding target ticket to use
            ResourceTicket<T> t = findNextAcceptableTicket(targetIt, targetSelector);
            if (t == null) {
                break;
            }
            // connect found tickets to each other
            t.setSource(s);
        }
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
    
    protected ResourceTicket<T> insert(ResourceTicket ticket) {
        getTickets().add(ticket);
        ticket.setGroup(this);
        return ticket;
    }
    protected boolean remove(ResourceTicket<T> ticket) {
        if (getTickets().remove(ticket)) {
            ticket.setSource(null);
            ticket.clearAllTargets();
            ticket.setGroup(null);
            return true;
        }
        return false;
    }
    protected void clear() {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
            t.clearAllTargets();
            t.setGroup(null);
        }
        getTickets().clear();
    }
    protected ResourceTicket<T> findNextAcceptableTicket(Iterator<ResourceTicket<T>> it, TicketSelector selector) {
        while (it.hasNext()) {
            ResourceTicket<T> t = it.next();
            if (selector.test(t)) {
                return t;
            }
        }
        return null;
    }
    
}
