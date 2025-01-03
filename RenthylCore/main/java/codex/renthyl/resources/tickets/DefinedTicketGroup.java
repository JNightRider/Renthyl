/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.resources.ResourceList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author codex
 * @param <T>
 * @param <D>
 */
public interface DefinedTicketGroup <T, D extends ResourceDef<T>> extends TicketGroup<T> {
    
    public Collection<D> getDefs();
    
    public D getDef(int i);
    
    public void add(ResourceTicket<T> ticket, D def);
    
    public default ResourceTicket<T> add(String name, D def) {
        ResourceTicket<T> t = new ResourceTicket<>(name);
        add(t, def);
        return t;
    }
    
    public default void declareAll(ResourceList resources, RenderModule module) {
        Iterator<ResourceTicket<T>> tickets = getTickets().iterator();
        Iterator<D> defs = getDefs().iterator();
        while (tickets.hasNext() && defs.hasNext()) {
            resources.declare(module, defs.next(), tickets.next());
        }
    }
    
}
