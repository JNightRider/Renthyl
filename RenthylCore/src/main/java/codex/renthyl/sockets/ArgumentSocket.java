package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

public class ArgumentSocket <T> extends TransitiveSocket<T> {

    private T value;

    public ArgumentSocket(Renderable task) {
        this(task, null);
    }
    public ArgumentSocket(Renderable task, T value) {
        super(task);
        this.value = value;
    }

    @Override
    public T acquire() {
        T v = super.acquire();
        return v != null ? v : value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
