/*
 * Copyright (c) 2024, codex
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

import codex.renthyl.newresources.Socket;
import codex.renthyl.resources.Ticket;

/**
 * References a {@link RenderResource} by index.
 * <p>
 * Can reference another ticket as a source, which makes this point to the same
 * resource as the source ticket. This mechanism allows RenderPasses to share
 * resources. Also vaguely tracks the last seen render object, which is used to
 * prioritize that render object, especially for reservations.
 * 
 * @author codex
 * @param <T>
 */
public class ResourceTicket <T> implements Ticket<Integer, T>, Socket<ResourceTicket<T>> {

    private final String name;
    private int index = -1;
    private ResourceTicket<T> upstream;
    private Ticket<Long, T> resourceRef = Ticket.ticket(-1L);

    public ResourceTicket(String name) {
        this.name = name;
    }

    @Override
    public void setIndex(Integer index) {
        this.index = index;
    }
    @Override
    public Integer getIndex() {
        if (upstream != null) {
            return upstream.getIndex();
        }
        return index;
    }
    @Override
    public void setUpstream(ResourceTicket<T> upstream) {
        this.upstream = upstream;
    }

    public void setResourceRef(Ticket<Long, T> resourceRef) {
        this.resourceRef = resourceRef;
    }

    public String getName() {
        return name;
    }
    @Override
    public ResourceTicket<T> getUpstream() {
        return upstream;
    }
    public Ticket<Long, T> getResourceRef() {
        return resourceRef;
    }

}
