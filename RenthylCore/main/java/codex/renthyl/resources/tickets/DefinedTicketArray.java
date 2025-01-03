/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.resources.ResourceList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 *
 * @author codex
 * @param <T>
 * @param <D>
 */
public class DefinedTicketArray <T, D extends ResourceDef<T>> extends TicketArray<T>
        implements DefinedTicketGroup<T, D> {
    
    private final ArrayList<D> defs;
    
    public DefinedTicketArray(String name, D... defs) {
        super(name, defs.length);
        this.defs = new ArrayList<>(defs.length);
        this.defs.addAll(Arrays.asList(defs));
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
    public void add(ResourceTicket<T> ticket, D def) {
        throw new UnsupportedOperationException("Tickets cannot be added to this group.");
    }
    
}
