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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Handles a raw object used for rendering processes within a FrameGraph.
 * 
 * @author codex
 * @param <T>
 */
public class RenderObject <T> implements ResourceWrapper<T> {

    private final T resource;
    private final int refresh;
    private int timeout;
    private boolean available = false;
    private boolean checkout = false;
    private final Collection<TimeFrame> reservations = new ArrayList<>();

    public RenderObject(T resource, int timout) {
        this.resource = resource;
        this.refresh = this.timeout = timeout;
    }
    public RenderObject(ResourceDef<T> def) {
        this(def.createResource(), def.getStaticTimeout());
    }

    @Override
    public void reference(TimeFrame time) {

    }

    @Override
    public T acquire() {
        if (!available) {
            throw new IllegalStateException("Resource not available.");
        }
        this.available = false;
        this.timeout = refresh; // refresh
        return resource;
    }

    @Override
    public void release() {
        available = true;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public void checkout(boolean checkout) {
        this.checkout = checkout;
    }

    public boolean isCheckedOut() {
        return checkout;
    }

    public T acquire() {
        return resource;
    }

    public boolean cycleTimeout() {
        return --timeout <= 0;
    }

    public void addReservation(TimeFrame timeFrame) {
        reservations.add(timeFrame);
    }

    public boolean claimReservation(TimeFrame timeFrame) {
        for (Iterator<TimeFrame> it = reservations.iterator(); it.hasNext();) {
            TimeFrame t = it.next();
            if (t.equals(timeFrame)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean isReserved(TimeFrame timeFrame) {
        for (TimeFrame t : reservations) {
            if (t.overlaps(timeFrame)) {
                return true;
            }
        }
        return false;
    }

}
