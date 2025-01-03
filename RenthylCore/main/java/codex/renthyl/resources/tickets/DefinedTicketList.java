/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.definitions.ResourceDef;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author codex
 * @param <T>
 * @param <D>
 */
public class DefinedTicketList <T, D extends ResourceDef<T>> extends TicketList<T>
        implements DefinedTicketGroup<T, D> {
    
    private final ArrayList<D> defs = new ArrayList<>();
    
    public DefinedTicketList(String name) {
        super(name);
    }

    @Override
    public Collection<D> getDefs() {
        return defs;
    }
    @Override
    public D getDef(int i) {
        return defs.get(i);
    }
    @Override
    public void add(ResourceTicket<T> ticket, D def)  {
        getDefs().add(def);
        super.add(ticket);
    }
    @Override
    public void add(ResourceTicket<T> ticket) {
        throw new UnsupportedOperationException("Tickets must be added along with a definition.");
    }
    
}
