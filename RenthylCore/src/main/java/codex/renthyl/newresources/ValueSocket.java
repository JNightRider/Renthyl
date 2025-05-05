package codex.renthyl.newresources;

public class ValueSocket <T> extends ModifyingSocket<T> {

    private T value;

    public ValueSocket(Renderable task) {
        super(task);
    }
    public ValueSocket(Renderable task, T value) {
        super(task);
        this.value = value;
    }

    @Override
    public T acquire() {
        return upstream != null ? upstream.acquire() : value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

}
