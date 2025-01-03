/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author codex
 * @param <T>
 */
public class TicketArray <T> extends AbstractTicketGroup<T> {
    
    private final ArrayList<ResourceTicket<T>> array;
    
    public TicketArray(String name, int size) {
        super(name);
        this.array = new ArrayList<>(size);
        fillArray(size);
    }
    public TicketArray(String name, String... tickets) {
        super(name);
        this.array = new ArrayList<>(tickets.length);
        fillArray(tickets);
    }
    
    private void fillArray(int size) {
        for (int i = 0; i < size; i++) {
            insert(new ResourceTicket<>(name + "[" + i + "]"));
        }
    }
    private void fillArray(String... tickets) {
        for (String ticket : tickets) {
            insert(new ResourceTicket<>(ticket));
        }
    }
    
    @Override
    public void add(ResourceTicket<T> ticket) {
        throw new UnsupportedOperationException("Tickets cannot be added to this group.");
    }
    @Override
    public Collection<ResourceTicket<T>> getTickets() {
        return array;
    }
    
    public ResourceTicket<T> get(int i) {
        return array.get(i);
    }
    
}
