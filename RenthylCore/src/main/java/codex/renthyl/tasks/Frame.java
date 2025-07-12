package codex.renthyl.tasks;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;

/**
 * Task that performs no rendering and its sockets can be safely interconnected.
 *
 * <p>Sockets are usually attached to a functional task that performs some kind
 * of rendering operation. The nature of such a task prohibits it from delegating
 * operations to child tasks. Doing so results in graphs that are impossible to render
 * in the correct order, as the child tasks technically depend on the parent task, and the
 * parent task technically depends on the child tasks.</p>
 *
 * <p>Frame solve this issue by {@link #skipRender() declining rendering} after being
 * staged and always have {@link #isRenderingComplete() completed rendering}. Although the
 * child tasks technically still depend on the Frame, the Frame never blocks the child tasks
 * from rendering. Frames act solely as a communication interface for its child tasks through
 * its sockets.</p>
 *
 * <p>Furthermore, Frame does not stage its sockets when it is staged. This means that a task
 * may be upstream of a Frame that is staged, but that task will not necessarily be staged
 * itself. In other words, Frame does not use incoming resources, so it does not require
 * that those tasks be rendered.</p>
 *
 * @author codex
 */
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
    public final boolean ready() {
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
    public final boolean skipRender() {
        return true;
    }

}
