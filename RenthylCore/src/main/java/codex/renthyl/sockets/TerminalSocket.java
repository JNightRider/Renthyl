package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class TerminalSocket <T> implements Socket<T> {

    protected final Renderable task;
    protected int activeRefs = 0;

    public TerminalSocket(Renderable task) {
        this.task = task;
    }

    @Override
    public boolean isAvailableToDownstream() {
        return task.isRenderingComplete();
    }

    @Override
    public boolean isUpstreamAvailable() {
        return true;
    }

    @Override
    public void queue(RenderingQueue queue) {
        task.queue(queue);
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

    @Override
    public int getResourceUsage() {
        return activeRefs;
    }

}
