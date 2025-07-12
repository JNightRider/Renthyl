package codex.renthyl;

import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.attributes.SynchronizedAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains global {@link Attribute Attributes} that can be referenced.
 *
 * @author codex
 */
public interface GlobalAttributes {

    /**
     * Gets the named global attribute.
     *
     * @param name
     * @return named attribute, or null
     * @param <T>
     */
    <T> Attribute<T> getAttribute(String name);

}
