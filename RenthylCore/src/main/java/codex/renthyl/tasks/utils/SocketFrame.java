package codex.renthyl.tasks.utils;

import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.Frame;

/**
 * Frame containing one socket.
 *
 * @param <T>
 */
public class SocketFrame <T extends Socket> extends Frame {

    private final T socket;

    public SocketFrame(T socket) {
        this.socket = addSocket(socket);
    }

    /**
     * Returns the one socket.
     *
     * @return
     */
    public T get() {
        return socket;
    }

}
