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
package codex.renthyl.tasks;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.macros.Macro;

/**
 * Interface pass between the framegraph and game logic, allowing them to communicate.
 * 
 * @author codex
 * @param <T>
 */
public class Attribute <T> extends AbstractTask implements Socket<T>, Macro<T> {

    private T value;
    private int activeRefs = 0;

    public Attribute() {}
    public Attribute(T value) {
        this.value = value;
    }

    @Override
    protected void renderTask() {}

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return true;
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return true;
    }

    @Override
    public T acquire() {
        return value;
    }

    @Override
    public void resetSocket() {
        if (activeRefs > 0) {
            throw new IllegalStateException("More references than releases.");
        }
    }

    @Override
    public int getResourceUsage() {
        return activeRefs;
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public T preview() {
        return value;
    }

    public void setValue(T value) {
        assertNoActiveReferences();
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
