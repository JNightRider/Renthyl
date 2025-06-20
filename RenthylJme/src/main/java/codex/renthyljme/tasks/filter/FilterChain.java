package codex.renthyljme.tasks.filter;

import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.Frame;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Frame which automatically connects {@link PostProcessFilter PostProcessFilters} together into
 * a standard filter chain.
 *
 * @author codex
 */
public class FilterChain extends Frame implements PostProcessFilter, Iterable<PostProcessFilter> {

    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> depth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> result = new TransitiveSocket<>(this);
    private final List<PostProcessFilter> filters = new ArrayList<>();

    public FilterChain() {
        addSockets(color, depth, result);
        result.setUpstream(color);
    }

    @Override
    public PointerSocket<Texture2D> getSceneColor() {
        return color;
    }

    @Override
    public PointerSocket<Texture2D> getSceneDepth() {
        return depth;
    }

    @Override
    public Socket<Texture2D> getFilterResult() {
        return result;
    }

    @Override
    public Iterator<PostProcessFilter> iterator() {
        return filters.iterator();
    }

    /**
     * Adds the filter to the end of the filter chain.
     *
     * @param filter filter to add and connect
     * @return added filter
     * @param <T>
     */
    public <T extends PostProcessFilter> T add(T filter) {
        return add(filters.size(), filter);
    }

    /**
     * Inserts the filter at {@code i} index in the filter chain.
     *
     * @param i position to add the filter to (must be in bounds)
     * @param filter filter to add and connect
     * @return added filter
     * @param <T>
     */
    public <T extends PostProcessFilter> T add(int i, T filter) {
        if (i > 0) {
            filter.getSceneColor().setUpstream(filters.get(i - 1).getFilterResult());
        } else {
            filter.getSceneColor().setUpstream(color);
        }
        filter.getSceneDepth().setUpstream(depth);
        if (i < filters.size()) {
            filters.get(i).getSceneColor().setUpstream(filter.getFilterResult());
        } else {
            result.setUpstream(filter.getFilterResult());
        }
        filters.add(i, filter);
        return filter;
    }

    /**
     * Inserts {@code filter} in the position just in front of {@code before}.
     *
     * @param filter filter to add
     * @param before filter to add {@code filter} just before
     * @return added filter
     * @param <T>
     */
    public <T extends PostProcessFilter> T addBefore(T filter, PostProcessFilter before) {
        int i = filters.indexOf(before);
        if (i < 0) {
            throw new IllegalArgumentException(before + " is not part of the chain.");
        }
        return add(i, filter);
    }

    /**
     * Inserts {@code filter} in the position just behind {@code after}.
     *
     * @param filter filter to add
     * @param after filter to add {@code filter} just after
     * @return added filter
     * @param <T>
     */
    public <T extends PostProcessFilter> T addAfter(T filter, PostProcessFilter after) {
        int i = filters.indexOf(after);
        if (i < 0) {
            throw new IllegalArgumentException(after + " is not part of the chain.");
        }
        return add(i + 1, filter);
    }

    /**
     * Removes and disconnects {@code filter} from the chain.
     *
     * @param filter
     */
    public void remove(PostProcessFilter filter) {
        int i = filters.indexOf(filter);
        if (i >= 0) {
            filters.remove(i);
            if (filters.isEmpty()) {
                result.setUpstream(color);
            } else if (i == 0) {
                filters.get(i).getSceneColor().setUpstream(color);
            } else if (i == filters.size()) {
                result.setUpstream(filters.get(i - 1).getFilterResult());
            } else {
                filters.get(i).getSceneColor().setUpstream(filters.get(i - 1).getFilterResult());
            }
        }
    }

    /**
     * Clears all filters from this chain.
     */
    public void clear() {
        filters.clear();
        result.setUpstream(color);
    }

    /**
     * Gets the filter at the index in the chain.
     *
     * @param i index of filter (must be in bounds)
     * @return filter at the index
     */
    public PostProcessFilter get(int i) {
        return filters.get(i);
    }

    /**
     * Gets the filter of {@code type} at the index in the chian.
     *
     * @param i index of filter (must be in bounds)
     * @param type type of filter
     * @return indexed filter
     * @param <T>
     * @throws ClassCastException if the indexed filter is not of {@code type}
     */
    public <T extends PostProcessFilter> T get(int i, Class<T> type) {
        return type.cast(get(i));
    }

    /**
     * Returns the first filter in the chain of {@code type}.
     *
     * @param type type of filter to find
     * @return first filter of {@code type}
     * @param <T>
     */
    public <T extends PostProcessFilter> T get(Class<T> type) {
        return filters.stream().filter(f -> type.isAssignableFrom(f.getClass())).map(f -> (T)f).findFirst().orElse(null);
    }

    /**
     * Returns the number of filters in this chain.
     *
     * @return
     */
    public int size() {
        return filters.size();
    }

}
