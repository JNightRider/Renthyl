package codex.renthyl.newresources;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DynamicSocketList <T extends Socket<R>, R> extends SocketList<T, R> {

    private final List<T> sockets = new ArrayList<>();
    private final Supplier<T> factory;

    public DynamicSocketList(Renderable task, Supplier<T> factory) {
        super(task);
        this.factory = factory;
    }

    public void addUpstream(Socket<R> upstream) {
        T s = factory.get();
        s.setUpstream(upstream);
        sockets.add(s);
    }

    public void removeUpstream(Socket upstream) {
        sockets.removeIf(s -> {
            if (s.getUpstream() == upstream) {
                s.setUpstream(null);
                return true;
            }
            return false;
        });
    }

    public void flushTerminalSockets() {
        removeUpstream(null);
    }

}
