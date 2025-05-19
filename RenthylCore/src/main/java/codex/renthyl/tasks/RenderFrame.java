package codex.renthyl.tasks;

import codex.renthyl.render.RenderWorker;

public class RenderFrame extends RenderTask {

    @Override
    public final void prepare() {}

    @Override
    public boolean claim(RenderWorker worker) {
        throw new UnsupportedOperationException("Does not directly support rendering.");
    }

    @Override
    public final void render() {
        throw new UnsupportedOperationException("Does not directly support rendering.");
    }

    @Override
    public boolean isRenderingComplete() {
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
