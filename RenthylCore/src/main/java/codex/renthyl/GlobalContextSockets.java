package codex.renthyl;

import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.SocketMap;
import codex.renthyl.tasks.Frame;

public class GlobalContextSockets extends Frame {

    public static final String CONTEXT = "framegraphcontext";

    private final SocketMap<String, ArgumentSocket<Object>, Object> globals = new SocketMap<>(this);

    public GlobalContextSockets() {
        addSocket(globals);
    }

    public void setGlobalValue(String name, Object value) {
        globals.computeIfAbsent(name, k -> new ArgumentSocket<>(this)).setValue(value);
    }

    public <T> void attachToGlobal(String name, PointerSocket<T> socket) {
        Socket<Object> global = globals.get(name);
        if (global != socket.getUpstream()) {
            socket.setUpstream((Socket<T>)global);
        }
    }

    public <T> Socket<T> getGlobal(String name, Class<T> type) {
        return (Socket<T>)globals.get(name);
    }

    public SocketMap<String, ? extends Socket<Object>, Object> getGlobals() {
        return globals;
    }

}
