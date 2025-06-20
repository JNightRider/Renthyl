package codex.renthyl.render;

/**
 * Tracks the history of a rendering context setting for the purpose
 * of precisely undoing changes to that setting.
 *
 * @param <T>
 */
public interface ContextSetting<T> {

    /**
     * Pushes the current setting value onto the history stack.
     *
     * @see #pop()
     */
    void push();

    /**
     * Pops the top value off the history stack (if able) and applies
     * it to the setting.
     */
    void pop();

    /**
     * Applies the bottom value of the stack to the setting, then clears
     * the stack.
     */
    void reset();

    /**
     * Applies {@code value} to the setting.
     *
     * @param value
     */
    void setValue(T value);

    /**
     * Gets the current setting value.
     *
     * @return
     */
    T getValue();

    /**
     * {@link #push() Pushes} the current setting value, then applies {@code value}
     * to the setting.
     *
     * @param value
     */
    default void pushValue(T value) {
        push();
        setValue(value);
    }

}
