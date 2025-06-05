package codex.renthyl.render.queue;

import codex.renthyl.GlobalAttributes;

/**
 * Object that can possibly be staged into a {@link RenderingQueue}.
 *
 * <p>Although an object may implement this interface, it does not mean
 * the object can actually be added to a RenderingQueue. This only gives
 * the object <em>a chance</em> to be added if desired, or possibly stage
 * other objects.</p>
 *
 * <p>For example, {@link codex.renthyl.sockets.Socket Socket} extends this
 * interface for the purpose of staging the {@link codex.renthyl.render.Renderable Renderables}
 * they are associated with. Not to be added to a RenderingQueue themselves.</p>
 */
public interface Stageable {

    /**
     * Gives this Stageable an opportunity to be staged into the {@code queue}.
     * Whether this actually becomes staged into {@code queue} depends on the
     * implementation.
     *
     * @param globals
     * @param queue
     */
    void stage(GlobalAttributes globals, RenderingQueue queue);

}
