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
public class TicketList <T> extends AbstractTicketGroup<T> {
    
    private final ArrayList<ResourceTicket<T>> tickets = new ArrayList<>();
    
    public TicketList(String name) {
        super(name);
    }

    @Override
    public Collection<ResourceTicket<T>> getTickets() {
        return tickets;
    }
    
}
