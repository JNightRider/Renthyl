package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;

/**
 * Socket returning a value that can be treated mutably.
 *
 * @param <T>
 * @author codex
 */
public class ValueSocket <T> extends ModifyingSocket<T> {

    private T value;

    /**
     * Creates a ValueSocket with a null internal value.
     *
     * @param task underlying task
     */
    public ValueSocket(Renderable task) {
        super(task);
    }

    /**
     *
     * @param task underlying task
     * @param value initial value
     */
    public ValueSocket(Renderable task, T value) {
        super(task);
        this.value = value;
    }

    @Override
    public T acquire() {
        return upstream != null ? upstream.acquire() : value;
    }

    /**
     * Sets the internal value returned by {@link #acquire()} if
     * no upstream socket is defined.
     *
     * @param value
     */
    public void setValue(T value) {
        this.value = value;
    }

    /**
     * Gets the internal value.
     *
     * @return
     */
    public T getValue() {
        return value;
    }

}
