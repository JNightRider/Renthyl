package codex.renthyl.sockets.macros;

import codex.renthyl.sockets.Socket;

/**
 * Interfacing providing a preview of a particular value. This is used
 * most often in combination with {@link codex.renthyl.sockets.Socket sockets}
 * to provide access to a value outside the acceptable stages (where accessing
 * through {@link Socket#acquire()} would be illegal).
 *
 * @param <T>
 * @author codex
 */
public interface Macro <T> {

    /**
     * Returns a preview value. The returned value is not guaranteed to remain
     * the same over the entire pipeline.
     *
     * @return preview value
     */
    T preview();

}
