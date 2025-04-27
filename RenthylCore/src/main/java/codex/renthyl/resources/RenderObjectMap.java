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
package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.newresources.ResourceAllocator;
import codex.renthyl.newresources.ResourceReceiver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages creation, reallocation, and disposal of {@link RenderObject RenderObjects}
 * globally across all {@link codex.renthyl.FrameGraph FrameGraphs}.
 * 
 * @author codex
 */
public class RenderObjectMap implements ResourceAllocator {

    private final Map<Long, RenderObject<?>> map = new ConcurrentHashMap<>();

    @Override
    public void allocate(ResourceReceiver shell, ResourceDef<?> def, TimeFrame timeFrame) {
        final EvaluatedResource eval = new EvaluatedResource();
        final Queue<RenderObject<?>> skipped = new LinkedList<>();
        final Iterator<RenderObject<?>> iterator = map.values().iterator();
        while (iterator.hasNext() || skipped.isEmpty()) {
            RenderObject<?> obj = iterator.hasNext() ? iterator.next() : skipped.poll();
            if (!obj.isAvailable()) {
                continue;
            }
            if (obj.isCheckedOut()) {
                skipped.add(obj);
                continue;
            }
            synchronized (obj) {
                if (!obj.isAvailable()) {
                    continue;
                }
                obj.checkout(true);
                if (obj.claimReservation(timeFrame) || !obj.isReserved(timeFrame)) {
                    eval.add(obj, def.evaluateResource(obj.acquire()));
                    if (eval.isFinal()) {
                        shell.receiveResource(obj);
                        obj.checkout(false);
                        return;
                    }
                }
                obj.checkout(false);
            }
        }
        if (!eval.isNull() && eval.getResource().isAvailable()) {
            synchronized (eval.getResource()) {
                if (eval.getResource().isAvailable()) {

                }
            }
        }
    }

    @Override
    public void allocate(ResourceReceiver shell, ResourceDef<?> def, TimeFrame timeFrame, Ticket<Long, ?> ticket) {
        return null;
    }

    @Override
    public void reserve(Ticket<Long, ?> ticket, TimeFrame timeFrame) {
        RenderObject<?> obj = map.get(ticket.getIndex());
        if (obj != null) obj.addReservation(timeFrame);
    }

    @Override
    public void flush() {
        for (Iterator<Map.Entry<Long, RenderObject<?>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Long, RenderObject<?>> entry = it.next();
            if (!entry.getValue().isAvailable()) {
                throw new IllegalStateException("Resource " + entry.getKey() + " not released.");
            }
            if (entry.getValue().cycleTimeout()) {
                it.remove();
            }
        }
    }

}
