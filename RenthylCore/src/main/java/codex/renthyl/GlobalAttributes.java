package codex.renthyl;

import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.attributes.SynchronizedAttribute;

import java.util.HashMap;
import java.util.Map;

public class GlobalAttributes {

    private final Map<String, Attribute<Object>> globals = new HashMap<>();

    public void set(String name, Object value) {
        set(name, value, true);
    }

    public void setSynchronized(String name, Object value) {
        set(name, value, false);
    }

    public void set(String name, Object value, boolean async) {
        Attribute attr = globals.get(name);
        if (attr == null) {
            attr = async ? new Attribute() : new SynchronizedAttribute();
            globals.put(name, attr);
        }
        attr.setValue(value);
    }

    public <T> Attribute<T> get(String name) {
        return (Attribute<T>)globals.get(name);
    }

}
