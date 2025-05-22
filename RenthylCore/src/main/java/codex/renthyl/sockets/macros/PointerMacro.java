package codex.renthyl.sockets.macros;

public interface PointerMacro <T> extends Macro<T> {

    void setUpstream(Macro<? extends T> upstream);

    Macro<? extends T> getUpstream();

}
