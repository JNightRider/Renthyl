package codex.renthyl.sockets;

/**
 * Socket that may point to one upstream socket of the same or similar resource type.
 *
 * @param <T> resource type
 * @author codex
 */
public interface PointerSocket <T> extends Socket<T> {

    /**
     * Sets the upstream socket to point to.
     *
     * @param upstream upstream socket (may be null)
     */
    void setUpstream(Socket<? extends T> upstream);

    /**
     * Gets the current upstream socket.
     *
     * @return upstream socket
     */
    Socket<? extends T> getUpstream();

}
