package codex.renthyl.tasks;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.RenderWorker;
import codex.renthyl.render.queue.RenderingQueue;

public class Frame extends AbstractTask {

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (position < QUEUED) {
            position = queue.stage(this);
        }
    }

    @Override
    public final void prepare() {
        if (position < QUEUED) {
            throw new IllegalStateException("Frame is being prepared, but is not properly staged.");
        }
    }

    @Override
    public final boolean claim(RenderWorker worker) {
        throw new UnsupportedOperationException("Does not directly support rendering.");
    }

    @Override
    public final void render() {
        throw new UnsupportedOperationException("Does not directly support rendering.");
    }

    @Override
    public final boolean isRenderingComplete() {
        return true;
    }

    @Override
    protected final void renderTask() {
        throw new UnsupportedOperationException("Does not directly support rendering.");
    }

    @Override
    public final boolean queueForRender() {
        return false;
    }

}
