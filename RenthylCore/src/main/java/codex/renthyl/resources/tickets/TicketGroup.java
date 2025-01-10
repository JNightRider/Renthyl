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

import codex.renthyl.export.TicketIndex;
import codex.renthyl.modules.LayoutMember;
import codex.renthyl.resources.ResourceList;
import java.util.Collection;
import codex.renthyl.modules.Connectable;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Contains a set of {@link ResourceTicket ResourceTickets} organized under
 * a single name within a {@link Connectable}.
 * 
 * @author codex
 * @param <T>
 */
public interface TicketGroup <T> extends LayoutMember, Iterable<ResourceTicket<T>> {
    
    /****************
     * Abstract API *
     ****************/
    
    /**
     * Returns the name of this group.
     * 
     * @return 
     */
    public String getName();
    
    /**
     * Assigns this group to the owner.
     * <p>
     * If this group is already assigned to an owner,
     * an exception will be thrown.
     * 
     * @param owner 
     */
    public void attach(Connectable owner);
    
    /**
     * Cleans up the group.
     * <p>
     * Called if the group is removed from its owner or the owner is
     * removed from the framegraph.
     */
    public void detach();
    
    /**
     * Adds a new ticket with {@code name} to this group.
     * 
     * @param name
     * @return new ticket
     */
    public ResourceTicket<T> add(String name);
    
    /**
     * Gets all tickets in this group.
     * 
     * @return 
     */
    public Collection<ResourceTicket<T>> getTickets();
    
    /**
     * Connects each target ticket accepted by the target selector to a
     * corresponding source ticket accepted by the source selector.
     * 
     * @param source
     * @param sourceSelector
     * @param targetSelector 
     * @return the number of connections made as a result of this call
     */
    public int makeInput(TicketGroup<T> source, TicketSelector sourceSelector, TicketSelector targetSelector);
    
    /**
     * Gets the number of tickets in the group that have a
     * non-null source.
     * 
     * @return 
     */
    public int getNumConnectedTickets();
    
    /**
     * 
     * @param registry
     * @param indices 
     */
    public void applySavedConnections(Map<Integer, Connectable> registry, Collection<TicketIndex> indices);
    
    /**
     * 
     * @param setup
     */
    public void generateExportIndices(Consumer<TicketIndex> setup);
    
    /*******************
     * Implemented API *
     *******************/
    
    /**
     * Gets the first ticket accepted by the selector.
     * 
     * @param selector
     * @return 
     */
    public default ResourceTicket<T> select(TicketSelector selector) {
        return selector.selectFrom(getTickets());
    }
    
    /**
     * Adds each selected ticket in this group to {@code collection}.
     * 
     * @param <R>
     * @param selector
     * @param collection
     * @return collection containing selected tickets
     */
    public default <R extends Collection<ResourceTicket<T>>> R select(TicketSelector selector, R collection) {
        return selector.selectFrom(getTickets(), collection);
    }
    
    /**
     * Returns the size of this group.
     * 
     * @return 
     */
    public default int size() {
        return getTickets().size();
    }
    
    /**
     * Called when the source of a ticket in this group is changing.
     * 
     * @param ticket ticket whose source is changing
     * @param source the new source that will be assigned to the ticket
     */
    public default void ticketSourceChanged(ResourceTicket<T> ticket, ResourceTicket<T> source) {
        setLayoutUpdateNeeded();
    }
    
    /**
     * Connects the named target ticket from this group to the named source
     * ticket from the source group.
     * 
     * @param source
     * @param sourceName
     * @param targetName 
     * @return 
     */
    public default int makeInput(TicketGroup<T> source, String sourceName, String targetName) {
        return makeInput(source, TicketSelector.name(sourceName), TicketSelector.name(targetName));
    }
    
    /**
     * Connects all named target tickets from this group to the corresponding
     * named source tickets from the source group.
     * <p>
     * The source names and target names are intertwined within the same array
     * {@code names}. Every other name is expected to be a source name, with the
     * corresponding target name being directly after.
     * 
     * @param source
     * @param names
     * @throws IllegalArgumentException if an odd number of names is passed
     */
    public default void makeAllInput(TicketGroup<T> source, String... names) {
        if ((names.length & 1) == 1) {
            throw new IllegalArgumentException("An even number of names is required (found " + names.length + ").");
        }
        for (int i = 0; i < names.length; i += 2) {
            makeInput(source, TicketSelector.name(names[i]), TicketSelector.name(names[i + 1]));
        }
    }
    
    /**
     * 
     * @param selector 
     */
    public default void clearInput(TicketSelector selector) {
        for (ResourceTicket<T> t : getTickets()) {
            t.setSource(null);
        }
    }
    
    /**
     * Releases all resources associated with all tickets in this group.
     * 
     * @param resources 
     */
    public default void releaseAll(ResourceList resources) {
        for (ResourceTicket<T> t : getTickets()) {
            resources.releaseOptional(t);
        }
    }
    
    /**
     * Creates a stream of the tickets in this group.
     * 
     * @return 
     */
    public default Stream<ResourceTicket<T>> stream() {
        return getTickets().stream();
    }
    
}
