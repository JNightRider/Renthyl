package codex.renthyl.sockets.macros;

/**
 * Macro extension that references an upstream macro, usually for
 * previewing.
 *
 * @param <T>
 * @author codex
 */
public interface PointerMacro <T> extends Macro<T> {

    void setUpstream(Macro<? extends T> upstream);

    Macro<? extends T> getUpstream();

}
