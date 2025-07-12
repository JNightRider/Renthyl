package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;

/**
 * Listens for rendering events.
 */
public interface RenderingListener {

    /**
     * Called when a task begins rendering.
     *
     * @param task
     */
    void onTaskBegin(Renderable task);

    /**
     * Called when a task finishes rendering.
     *
     * @param task
     */
    void onTaskEnd(Renderable task);

}
