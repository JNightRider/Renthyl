/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.resources.tickets;

import java.util.Collection;
import java.util.function.Predicate;

/**
 *
 * @author codex
 */
public interface TicketSelector extends Predicate<ResourceTicket> {
    
    @Override
    public boolean test(ResourceTicket t);
    
    /**
     * Returns the first selected ticket in the collection.
     * 
     * @param <T>
     * @param tickets
     * @return first selected ticket, or null
     */
    public default <T> ResourceTicket<T> selectFrom(Collection<ResourceTicket<T>> tickets) {
        for (ResourceTicket<T> t : tickets) {
            if (test(t)) {
                return t;
            }
        }
        return null;
    }
    
    /**
     * Adds all selected tickets from {@code tickets} to {@code selected}.
     * 
     * @param <T>
     * @param <R> collection type to hold selected tickets
     * @param tickets collection to select tickets from
     * @param selected collection to add selected tickets to
     * @return collection containing selected tickets
     */
    public default <T, R extends Collection<ResourceTicket<T>>> R selectFrom
            (Collection<ResourceTicket<T>> tickets, R selected) {
        for (ResourceTicket t : tickets) {
            if (test(t)) {
                selected.add(t);
            }
        }
        return selected;
    }
    
    /**
     * Selector that selects all tickets.
     */
    public static final TicketSelector All = t -> true;
    
    /**
     * Creates a selector that selects only the given ticket.
     * 
     * @param ticket
     * @return 
     */
    public static TicketSelector is(ResourceTicket ticket) {
        return t -> t == ticket;
    }
    /**
     * Creates a selector that selects tickets with an equal name.
     * 
     * @param name
     * @return 
     */
    public static TicketSelector name(String name) {
        return t -> name.equals(t.getName());
    }
    /**
     * Creates a selector that selects tickets with a name equal
     * to any of the given names.
     * 
     * @param names
     * @return 
     */
    public static TicketSelector anyName(String... names) {
        return new AnyNameSelector(names);
    }
    /**
     * Creates a selector that selects only the first ticket (index 0).
     * 
     * @return 
     */
    public static TicketSelector first() {
        return new IndexBetweenSelector(0, 1);
    }
    /**
     * Creates a selector that selects only the ticket at the index.
     * 
     * @param index
     * @return 
     */
    public static TicketSelector at(int index) {
        return new IndexBetweenSelector(index, index + 1);
    }
    /**
     * Creates a selector that selects tickets at or after the index.
     * 
     * @param index
     * @return 
     */
    public static TicketSelector atOrAfter(int index) {
        return new IndexBetweenSelector(index, Integer.MAX_VALUE);
    }
    /**
     * Creates a selector that selects tickets before the index.
     * 
     * @param index
     * @return 
     */
    public static TicketSelector before(int index) {
        return new IndexBetweenSelector(0, index);
    }
    /**
     * Creates a selector that selects tickets between the start index
     * (inclusive) and end index (exclusive).
     * 
     * @param start
     * @param end
     * @return 
     */
    public static TicketSelector between(int start, int end) {
        return new IndexBetweenSelector(start, end);
    }
    
    public static class AnyNameSelector implements TicketSelector {
        
        private final String[] names;

        public AnyNameSelector(String[] names) {
            this.names = names;
        }
        
        @Override
        public boolean test(ResourceTicket t) {
            for (String n : names) {
                if (n.equals(t.getName())) {
                    return true;
                }
            }
            return false;
        }
        
    }
    public static class IndexBetweenSelector implements TicketSelector {
        
        private final int start, end;
        private int index = -1;

        public IndexBetweenSelector(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        @Override
        public boolean test(ResourceTicket t) {
            index++;
            return index >= start && index < end;
        }
        
    }
    
}
