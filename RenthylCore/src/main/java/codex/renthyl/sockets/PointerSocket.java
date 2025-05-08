package codex.renthyl.sockets;

public interface PointerSocket <T> extends Socket<T> {

    void setUpstream(Socket<T> upstream);

    Socket<T> getUpstream();

}
