package codex.renthyl.tasks;

import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FilterChain extends Frame implements PostProcessFilter, Iterable<PostProcessFilter> {

    private final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> depth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> result = new TransitiveSocket<>(this);
    private final List<PostProcessFilter> filters = new ArrayList<>();

    public FilterChain() {
        addSockets(color, depth, result);
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

    public <T extends PostProcessFilter> T add(T filter) {
        return add(filters.size(), filter);
    }

    public <T extends PostProcessFilter> T add(int i, T filter) {
        if (i > 0) {
            filter.getSceneColor().setUpstream(filters.get(i - 1).getFilterResult());
        } else {
            filter.getSceneDepth().setUpstream(color);
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

    public <T extends PostProcessFilter> T addBefore(T filter, PostProcessFilter before) {
        int i = filters.indexOf(before);
        if (i < 0) {
            throw new IllegalArgumentException(before + " is not part of the chain.");
        }
        return add(i, filter);
    }

    public <T extends PostProcessFilter> T addAfter(T filter, PostProcessFilter after) {
        int i = filters.indexOf(after);
        if (i < 0) {
            throw new IllegalArgumentException(after + " is not part of the chain.");
        }
        return add(i + 1, filter);
    }

    public PostProcessFilter get(int i) {
        return filters.get(i);
    }

    public <T extends PostProcessFilter> T get(int i, Class<T> type) {
        return type.cast(get(i));
    }

    public <T extends PostProcessFilter> T get(Class<T> type) {
        return filters.stream().filter(f -> type.isAssignableFrom(f.getClass())).map(f -> (T)f).findAny().orElse(null);
    }

    public int size() {
        return filters.size();
    }

}
