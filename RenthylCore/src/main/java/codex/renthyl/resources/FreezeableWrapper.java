package codex.renthyl.resources;

/**
 * Wrapper that can be frozen to avoid unexpected changes to the wrapper
 * and underlying resource.
 *
 * <p>The full effects of being frozen is generally up to the implementations,
 * but it is mandatory that the underlying resource remain constant: no reallocation
 * or disposing.</p>
 *
 * @param <T>
 */
public interface FreezeableWrapper<T> extends ResourceWrapper<T> {

    void freeze(boolean freeze);

}
