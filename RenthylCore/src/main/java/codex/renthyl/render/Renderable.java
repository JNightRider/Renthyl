package codex.renthyl.render;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.render.queue.Stageable;

/**
 * Performs a rendering task as part of a render graph pipeline.
 */
public interface Renderable extends Stageable {

    /**
     * Called prior to being {@link #stage(GlobalAttributes, RenderingQueue) staged},
     * and prior to any dependency tasks being staged.
     *
     * <p>A task sometimes wish to change which tasks it depends on. This is
     * the only opportunity to safely do so in the pipeline, because afterward
     * the socket that is to stage this task will already have staged one or
     * more dependency tasks.</p>
     *
     * @param globals
     */
    void preStage(GlobalAttributes globals);

    /**
     * Updates this task.
     *
     * @param tpf
     */
    void update(float tpf);

    /**
     * Prepares this task. This is expected to reference at this time all
     * upstream resources it plans to use during rendering.
     */
    void prepare();

    /**
     * Determines if this task is ready to be rendered.
     *
     * @return
     */
    boolean ready();

    /**
     * Renders this task. All referenced resources are assumed to be fully accessible.
     */
    void render();

    /**
     * Returns true if this task has fully completed {@link #render() rendering} since the
     * last {@link #reset() reset}.
     *
     * @return
     */
    boolean isRenderingComplete();

    /**
     * Resets this task.
     */
    void reset();

    /**
     * Returns true if the Renderable should skip {@link RenderingQueue#render() rendering} when staged.
     * Can result in rendering deadlocks if other tasks depend on this task running first.
     *
     * @return
     */
    default boolean skipRender() {
        return false;
    }

}
