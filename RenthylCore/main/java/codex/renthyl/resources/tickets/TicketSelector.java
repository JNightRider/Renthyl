/*
 * Copyright (c) 2025, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.resources.tickets;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Interface for selecting tickets from a group.
 * 
 * @author codex
 */
public interface TicketSelector {
    
    /**
     * Returns true if {@code ticket} is selected in relationship to
     * {@code other} and the current {@code index}.
     * <p>
     * Criteria that evaluates based on {@code other} should always return
     * true if {@code other} is null. Selectors are often used to select
     * tickets from a collection without an {@code other} ticket to compare with.
     * 
     * @param ticket ticket to select
     * @param other ticket to compare with (usually for connecting)
     * @param index index of the ticket to select
     * @return true if the ticket is selected by this selector.
     */
    public boolean select(ResourceTicket ticket, ResourceTicket other, int index);
    
    /**
     * 
     * @param ticket
     * @param index
     * @return 
     */
    public default boolean select(ResourceTicket ticket, int index) {
        return TicketSelector.this.select(ticket, null, index);
    }
    
    /**
     * Returns the first selected ticket in the collection.
     * 
     * @param <T>
     * @param tickets
     * @return first selected ticket, or null
     */
    public default <T> ResourceTicket<T> selectFrom(Iterable<ResourceTicket<T>> tickets) {
        int i = 0;
        for (ResourceTicket<T> t : tickets) {
            if (TicketSelector.this.select(t, i++)) {
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
    public default <T, R extends Collection<ResourceTicket<T>>> R selectFrom(Iterable<ResourceTicket<T>> tickets, R selected) {
        int i = 0;
        for (ResourceTicket t : tickets) {
            if (TicketSelector.this.select(t, i++)) {
                selected.add(t);
            }
        }
        return selected;
    }
    
    /**
     * 
     * @param <T>
     * @param <R>
     * @param tickets
     * @param selected
     * @param orElse
     * @return 
     */
    public default <T, R extends Collection<ResourceTicket<T>>> R selectFromOrElse(Iterable<ResourceTicket<T>> tickets, R selected, ResourceTicket<T> orElse) {
        int i = 0;
        for (ResourceTicket t : tickets) {
            if (TicketSelector.this.select(t, i++)) {
                selected.add(t);
            } else {
                selected.add(orElse);
            }
        }
        return selected;
    }
    
    /**
     * Selector that selects all tickets.
     */
    public static final TicketSelector All = (t, o, i) -> true;
    
    /**
     * Creates a selector that selects only the given ticket.
     * 
     * @param ticket
     * @return 
     */
    public static TicketSelector is(ResourceTicket ticket) {
        return (t, o, i) -> t == ticket;
    }
    /**
     * Creates a selector that selects tickets that are within
     * the group.
     * 
     * @param group
     * @return 
     */
    public static TicketSelector group(TicketGroup group) {
        return (t, o, i) -> t.getGroup() == group;
    }
    /**
     * Creates a selector that selects tickets with an equal name.
     * 
     * @param name
     * @return 
     */
    public static TicketSelector name(String name) {
        return (t, o, i) -> name.equals(t.getName());
    }
    /**
     * Creates a selector that selects tickets with a name equal
     * to any of the given names.
     * 
     * @param names
     * @return 
     */
    public static TicketSelector names(String... names) {
        return new AnyNameSelector(names);
    }
    /**
     * Creates a selector that only confirms a relationship if the two
     * tickets' names are equal.
     * 
     * @param delegate
     * @return 
     */
    public static TicketSelector namesMatch() {
        return (t, o, i) -> o == null || t.getName().equals(o.getName());
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
    /**
     * Creates a selector that selects each ticket if all delegate
     * selectors also select it.
     * 
     * @param delegates
     * @return 
     */
    public static TicketSelector and(TicketSelector... delegates) {
        return new LogicSelector(delegates) {
            @Override
            protected boolean evaluateCounts(int trueCount, int falseCount) {
                return falseCount == 0;
            }
        };
    }
    /**
     * Creates a selector that selects tickets if any one delegate selector
     * also selects them.
     * 
     * @param delegates
     * @return 
     */
    public static TicketSelector or(TicketSelector... delegates) {
        return new LogicSelector(delegates) {
            @Override
            protected boolean evaluateCounts(int trueCount, int falseCount) {
                return trueCount > 0;
            }
        };
    }
    /**
     * Creates a select that selects each ticket only if no delegate
     * selectors select it.
     * 
     * @param delegates
     * @return 
     */
    public static TicketSelector nor(TicketSelector... delegates) {
        return new LogicSelector(delegates) {
            @Override
            protected boolean evaluateCounts(int trueCount, int falseCount) {
                return trueCount == 0;
            }
        };
    }
    
    public static class AnyNameSelector implements TicketSelector {
        
        private final String[] names;

        public AnyNameSelector(String[] names) {
            this.names = names;
        }
        
        @Override
        public boolean select(ResourceTicket t, ResourceTicket o, int i) {
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

        public IndexBetweenSelector(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        @Override
        public boolean select(ResourceTicket t, ResourceTicket o, int i) {
            return i >= start && i < end;
        }
        
    }
    public static abstract class LogicSelector implements TicketSelector {
        
        private final TicketSelector[] delegates;
        
        public LogicSelector(TicketSelector... delegates) {
            this.delegates = delegates;
        }
        
        @Override
        public boolean select(ResourceTicket ticket, ResourceTicket other, int index) {
            int trueCount = 0;
            int falseCount = 0;
            for (TicketSelector s : delegates) {
                if (s.select(ticket, other, index)) {
                    trueCount++;
                } else {
                    falseCount++;
                }
            }
            return evaluateCounts(trueCount, falseCount);
        }
        
        protected abstract boolean evaluateCounts(int trueCount, int falseCount);
        
    }
    
}
