/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.modules.NewConnectable;
import codex.renthyl.resources.ResourceTicket;
import java.util.Collection;

/**
 *
 * @author codex
 * @param <T>
 */
public interface TicketCollection <T> extends Iterable<ResourceTicket<T>> {
    
    public String getName();
    
    public void attach(NewConnectable owner);
    
    public void add(ResourceTicket<T> ticket);
    
    public void setLayoutUpdateNeeded();
    
    public Collection<ResourceTicket<T>> getTickets();
    
    public ResourceTicket<T> getTicket(int i);
    
    public ResourceTicket<T> getTicket(String name);
    
    public int size();
    
    public void flush();
    
    public default void disconnect() {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
            t.clearAllTargets();
        }
    }
    
}
