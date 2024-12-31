/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.resources.tickets.TicketCollection;
import java.util.HashMap;

/**
 *
 * @author codex
 */
public interface NewConnectable {
    
    /*
     * Connectable API.
     */
    
    public <T> ResourceTicket<T> addInput(ResourceTicket<T> ticket);
    
    public <T> ResourceTicket<T> addOutput(ResourceTicket<T> ticket);
    
    public <T> TicketCollection<T> addInputGroup(TicketCollection<T> group);
    
    public <T> TicketCollection<T> addOutputGroup(TicketCollection<T> group);
    
    public HashMap<String, TicketCollection> getInputGroups();
    
    public HashMap<String, TicketCollection> getOutputGroups();
    
    public TicketCollection getMainInputGroup();
    
    public TicketCollection getMainOutputGroup();
    
    /*
     * Default Implementations.
     */
    
    public default <T> ResourceTicket<T> addInput(String name) {
        return addInput(new ResourceTicket(name));
    }
    
    public default <T> ResourceTicket<T> addOutput(String name) {
        return addOutput(new ResourceTicket(name));
    }
    
}
