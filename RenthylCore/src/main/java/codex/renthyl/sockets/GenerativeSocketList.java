package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

import java.util.function.Supplier;

public class GenerativeSocketList <T extends Socket<R>, R> extends SocketList<T, R> {

    private final Supplier<T> factory;

    public GenerativeSocketList(Renderable task, Supplier<T> factory) {
        super(task);
        this.factory = factory;
    }

    public void fill(int size) {
        while (size() < size) {
            add(factory.get());
        }
    }

    public void clip(int size) {
        while (size() > size) {
            removeLast();
        }
    }

    public void set(int size) {
        fill(size);
        clip(size);
    }

}
