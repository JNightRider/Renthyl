package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

/**
 * Holds and returns a value that must be treated immutably.
 *
 * <p>If this socket's upstream is not defined, {@link #acquire()} returns
 * this socket's value instead.</p>
 *
 * @param <T>
 * @author codex
 */
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

    /**
     * Sets the value held by this socket, and returned by {@link #acquire()}
     * if no upstream socket is defined.
     *
     * @param value
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Gets the value held by this socket.
     *
     * @return
     */
    public T getValue() {
        return value;
    }

}
