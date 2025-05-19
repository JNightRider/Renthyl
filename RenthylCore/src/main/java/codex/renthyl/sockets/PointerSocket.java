package codex.renthyl.sockets;

public interface PointerSocket <T> extends Socket<T> {

    void setUpstream(Socket<? extends T> upstream);

    Socket<? extends T> getUpstream();

}
