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
package codex.renthyl.modules.protocol;

import codex.renthyl.modules.RenderModule;

/**
 * Interface for tagging extension interfaces as protocols.
 * <p>
 * A protocol is used to allow access to {@link codex.renthyl.resources.tickets.TicketSignature
 * signatures} via methods, which can then be used to connect to the
 * protocol's implementating module. For example, a module implementing
 * {@link FilterProtocol} is then guaranteed to have signatures for
 * color and depth input, and a result output.
 * <p>
 * The main benefit of implementing a protocol is that specialty
 * {@link codex.renthyl.modules.RenderContainer containers} can access
 * the tickets it requires without requiring that those tickets be stored
 * in specifically-named groups under specific names or at a specific
 * positions. Thus, protocols allow for greater flexibility in how modules
 * choose to format their tickets internally. However, due to this extra
 * flexibility protocols offer, signatures returned by one module should
 * not be used to access tickets from another module, as the correct
 * signatures for that module may be different.
 * <p>
 * Another benefit of protocols is they provide a rigid and unavoidable
 * API. Ticket and group names can easily be forgotten, and getting them
 * incorrect only produces a runtime error. Misusing protocols, on the
 * other hand, produces an immediate and clearly visible error.
 * <p>
 * Note that for methods returning a {@link codex.renthyl.resources.tickets.TicketSignature
 * signature}, protocol implementations are by default free to return
 * a {@link codex.renthyl.resources.tickets.TicketSignature#NULL null signature}
 * (which describes no existing ticket), unless otherwise specified by
 * the protocol.
 * 
 * @author codex
 */
public interface SignatureProtocol extends RenderModule {
    
    // This interface is used to tag its extension interfaces
    // as protocols, and provide documentation on the overall
    // concept of protocols. It has nothing to implement.
    
}
