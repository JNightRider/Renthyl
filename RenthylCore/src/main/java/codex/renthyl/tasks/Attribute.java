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

import codex.renthyl.FGRenderContext;
import codex.renthyl.client.GraphSource;
import codex.renthyl.client.GraphTarget;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.ValueSocket;

/**
 * Interface pass between the framegraph and game logic, allowing them to communicate.
 * <p>
 * Game logic can listen to framegraph parameters via {@link GraphTarget}s, and/or game logic
 * can communicate parameters to the framegraph via a {@link GraphSource}.
 * <p>
 * Objects handled by this pass are automatically marked as constant, so that future changes
 * do not taint the game logic's resource view.
 * <p>
 * Inputs:
 * <ul>
 *   <li>{@link #INPUT} ({@link Object}): the value to share with game logic via registered GraphTargets (optional).</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>{@link #OUTPUT} ({@link Object}): the value to share with the FrameGraph from game logic using the registered GraphSource</li>
 * </ul>
 * 
 * @author codex
 * @param <T>
 */
public class Attribute <T> extends RenderTask implements Socket<T> {

    private T value;
    private int activeRefs = 0;

    public Attribute() {}
    public Attribute(T value) {
        this.value = value;
    }

    @Override
    protected void renderTask(FGRenderContext context) {}

    @Override
    public boolean isAvailableToDownstream() {
        return isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return true;
    }

    @Override
    public T acquire() {
        return value;
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
    public void release() {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    public void setValue(T value) {
        assertNoActiveReferences();
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
