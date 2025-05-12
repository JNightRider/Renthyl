package codex.renthyl.render;

public interface ContextSetting<T> {

    void push();

    void pop();

    void reset();

    void setValue(T value);

    T getValue();

    default void pushValue(T value) {
        push();
        setValue(value);
    }

}
