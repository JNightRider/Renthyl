package codex.renthyl.tasks.utils;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.AbstractTask;

import java.util.function.Function;

public abstract class Derivative<In, Out> extends AbstractTask implements Socket<Out>, Function<In, Out> {

    private Socket<? extends In> upstream;
    private int activeRefs = 0;

    @Override
    protected void renderTask() {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || upstream.isAvailableToDownstream(queuePosition);
    }

    @Override
    public Out acquire() {
        if (upstream != null) {
            In v = upstream.acquire();
            return v != null ? apply(v) : null;
        } else return null;
    }

    @Override
    public int getResourceUsage() {
        return upstream == null ? activeRefs : Math.max(activeRefs, upstream.getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public void resetSocket() {}

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (position < QUEUING) {
            preStage(globals);
            position = QUEUING;
            if (upstream != null) {
                upstream.stage(globals, queue);
            }
            position = queue.stage(this);
        }
    }

    public void setUpstream(Socket<? extends In> upstream) {
        assertUnqueued();
        this.upstream = upstream;
    }

    public Socket<? extends In> getUpstream() {
        return upstream;
    }

}
