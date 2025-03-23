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

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceList;
import codex.renthyl.resources.ResourceUser;
import java.util.Collection;
import java.util.Iterator;

/**
 * TicketGroup that stores, for each ticket, a corresponding {@link ResourceDef}.
 * 
 * @author codex
 * @param <T>
 * @param <D>
 */
public interface DefinedTicketGroup <T, D extends ResourceDef<T>> extends TicketGroup<T> {
    
    /**
     * 
     * @return 
     */
    public Collection<D> getDefs();
    
    /**
     * 
     * @param i
     * @return 
     */
    public D getDef(String ticketName);
    
    /**
     * Adds a ticket along with its definition.
     * 
     * @param name
     * @param def 
     * @return  
     */
    public ResourceTicket<T> add(String name, D def);
    
    /**
     * {@link ResourceList#declare(codex.renthyl.resources.ResourceUser, codex.renthyl.definitions.ResourceDef, codex.renthyl.resources.tickets.ResourceTicket)
     * declares} using each ticket in this group along with the corresponding
     * {@link ResourceDef ResourceDefs}.
     * 
     * @param resources
     * @param user
     */
    public default void declareAll(ResourceList resources, ResourceUser user) {
        Iterator<ResourceTicket<T>> tickets = getTickets().iterator();
        Iterator<D> defs = getDefs().iterator();
        while (tickets.hasNext() && defs.hasNext()) {
            resources.declare(user, defs.next(), tickets.next());
        }
    }
    
    public default D selectDef(TicketSelector selector) {
        int i = 0;
        for (ResourceTicket<T> t : getTickets()) {
            if (selector.select(t, i)) {
                return getDef(t.getName());
            }
            i++;
        }
        return null;
    }
    
}
