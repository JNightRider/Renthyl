package codex.renthyl;

import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.attributes.SynchronizedAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Basic global attribute storage which supports both asynchronous {@link Attribute Attributes}
 * and {@link SynchronizedAttribute synchronized attributes}.
 */
public class BasicGlobalAttributes implements GlobalAttributes {

    private final Map<String, Attribute<Object>> globals = new HashMap<>();

    @Override
    public <T> Attribute<T> getAttribute(String name) {
        return (Attribute<T>)globals.get(name);
    }

    /**
     * Sets the value of the named attribute.
     *
     * <p>If the named attribute does not exist, a new attribute is created.</p>
     *
     * @param name
     * @param value
     */
    public void set(String name, Object value) {
        set(name, value, true);
    }

    /**
     * Sets the value of the named attribute.
     *
     * <p>If the named attribute does not exist, a new {@link SynchronizedAttribute} is created.</p>
     *
     * @param name
     * @param value
     */
    public void setSynchronized(String name, Object value) {
        set(name, value, false);
    }

    /**
     * Sets the value of the named attribute.
     *
     * <p>If the named attribute does not exist, a new attribute is created.</p>
     *
     * @param name
     * @param value
     * @param async if true a {@link SynchronizedAttribute} is created instead of a regular {@link Attribute}
     */
    public void set(String name, Object value, boolean async) {
        Attribute attr = globals.get(name);
        if (attr == null) {
            attr = async ? new Attribute() : new SynchronizedAttribute();
            globals.put(name, attr);
        }
        attr.setValue(value);
    }

}
