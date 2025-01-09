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

import java.util.ArrayList;
import java.util.Collection;

/**
 * A group containing a set number of tickets.
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
            append(new ResourceTicket<>(this, name + "[" + i + "]"));
        }
    }
    private void fillArray(String... tickets) {
        for (String ticket : tickets) {
            append(new ResourceTicket<>(this, ticket));
        }
    }
    
    @Override
    public ResourceTicket<T> add(String name) {
        throw new UnsupportedOperationException("Tickets cannot be added to this group.");
    }
    @Override
    public Collection<ResourceTicket<T>> getTickets() {
        return array;
    }
    
    /**
     * Gets the ticket at the index.
     * 
     * @param i
     * @return 
     */
    public ResourceTicket<T> get(int i) {
        return array.get(i);
    }
    
}
