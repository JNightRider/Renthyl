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
package codex.renthyl.tasks.geometry;

import codex.renthyl.FGRenderContext;
import codex.renthyl.GeometryQueue;
import codex.renthyl.sockets.*;
import codex.renthyl.tasks.RenderTask;

/**
 * Merges a specified number of {@link GeometryQueue}s into one output queue.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Queues[n] ({@link GeometryQueue}: queues to merge into one.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Result ({@link GeometryQueue}): resulting geometry queue.</li>
 * </ul>
 * 
 * @author codex
 */
public class QueueMergePass extends RenderTask {

    private final CollectorSocket<GeometryQueue> queues = new CollectorSocket<>(this);
    private final ValueSocket<GeometryQueue> result = new ValueSocket<>(this, new GeometryQueue());

    public QueueMergePass() {
        addSockets(queues, result);
    }

    @Override
    protected void renderTask(FGRenderContext context) {
        for (GeometryQueue g : queues.acquire()) {
            if (g != null) {
                result.getValue().add(g);
            }
        }
    }

    public CollectorSocket<GeometryQueue> getQueues() {
        return queues;
    }

    public Socket<GeometryQueue> getResult() {
        return result;
    }
    
}
