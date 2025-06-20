package codex.renthyl.render.queue;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import com.jme3.profile.AppProfiler;

import java.util.Collection;

/**
 * Processes and renders staged {@link Renderable tasks}.
 *
 *
 */
public interface RenderingQueue extends Iterable<Renderable> {

    /**
     * Stages the task by adding it to the end of the queue
     *
     * @param task task to stage
     * @return index of {@code task} in the queue once staged
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
     * Renders all staged tasks. Tasks may be rendered out of order relative to the order in which
     * they were staged.
     *
     * <p>Tasks only get rendered if they signal {@link Renderable#ready() ready}. If no unrendered tasks signal
     * ready, a deadlock occurs and an exception is thrown, depending on the implementor.</p>
     *
     * <p>Staged tasks may choose to {@link Renderable#skipRender() skip rendering}, in which case they do not
     * affect rendering at all. Deadlocking may occur as a result of skipping tasks.</p>
     */
    void render();

    /**
     * Resets all staged tasks, then removes all staged tasks.
     */
    void reset();

    /**
     * Performs all pipeline rendering steps.
     *
     * <p>The pipeline steps occur in this order:</p>
     * <ol>
     *     <li>Staging</li>
     *     <li>Update</li>
     *     <li>Preparation</li>
     *     <li>Render</li>
     *     <li>Reset</li>
     * </ol>
     *
     * @param globals
     * @param tasks
     * @param tpf
     */
    default void pipeline(GlobalAttributes globals, Iterable<Renderable> tasks, float tpf) {
        for (Renderable t : tasks) {
            t.stage(globals, this);
        }
        update(tpf);
        prepare();
        render();
        reset();
    }

}
