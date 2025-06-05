package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;
import com.jme3.profile.AppProfiler;

/**
 * Queue to which all {@link Renderable renderable tasks} are staged for rendering.
 *
 * <p>For {@link Iterable iteration}, all currently staged tasks must be iterated over, but
 * not necessarily in the order they were staged.</p>
 */
public interface RenderingQueue extends Iterable<Renderable> {

    /**
     * Stages the task by adding it to the end of the queue
     *
     * @param task task to stage
     * @return index of the task in the queue
     */
    int stage(Renderable task);

    /**
     * Updates all staged tasks.
     *
     * @param tpf
     */
    void update(float tpf);

    /**
     * Prepares all staged tasks.
     */
    void prepare();

    /**
     * Renders all staged tasks in the general order in which they were staged.
     *
     * <p>The exact rendering order depends on the implementation, and may be
     * erratic if rendering with multiple workers.</p>
     *
     * @param profiler application profiler (may be null)
     * @param workers number of working threads (must be positive)
     */
    void render(AppProfiler profiler, int workers);

    /**
     * Resets all staged tasks, then clears the queue.
     */
    void reset();

}
