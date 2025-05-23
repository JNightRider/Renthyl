package codex.renthyl;

import codex.renthyl.tasks.Attribute;
import codex.renthyl.tasks.SynchronizedAttribute;

import java.util.HashMap;
import java.util.Map;

public class GlobalAttributes {

    private final Map<String, Attribute<Object>> globals = new HashMap<>();

    public void set(String name, Object value) {
        set(name, value, true);
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
        Attribute attr = globals.get(name);
        if (attr == null) {
            throw new NullPointerException("Global attribute \"" + name + "\" is not defined.");
        }
        return (Attribute<T>)attr;
    }

}
