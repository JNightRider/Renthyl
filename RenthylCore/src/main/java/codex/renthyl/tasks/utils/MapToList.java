package codex.renthyl.tasks.utils;

import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.AbstractTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transfers elements acquired from an input Map into a List, either according to an
 * array of keys, or by iterating over the Map's entry set.
 *
 * @param <K> key type
 * @param <R> resource type
 */
public class MapToList<K, R> extends AbstractTask {

    private final K[] order;
    private final TransitiveSocket<Map<K, R>> map = new TransitiveSocket<>(this);
    private final ValueSocket<List<R>> list = new ValueSocket<>(this);

    public MapToList() {
        this((K[])null);
    }
    public MapToList(K[] order) {
        this(order, new ArrayList<>());
    }
    public MapToList(List<R> list) {
        this(null, list);
    }
    public MapToList(K[] order, List<R> list) {
        this.order = order;
        this.list.setValue(list);
        addSockets(map, this.list);
    }

    @Override
    protected void renderTask() {
        Map<K, R> inMap = map.acquire();
        if (order != null) {
            for (K key : order) {
                R r = inMap.get(key);
                if (r != null) {
                    list.getValue().add(r);
                }
            }
        } else for (R r : inMap.values()) {
            list.getValue().add(r);
        }
    }

    @Override
    public void reset() {
        super.reset();
        list.getValue().clear();
    }

    /**
     * Gets socket for incoming Map (input).
     *
     * @return
     */
    public PointerSocket<Map<K, R>> getMap() {
        return map;
    }

    /**
     * Gets socket for outgoing List (output).
     *
     * @return
     */
    public Socket<List<R>> getList() {
        return list;
    }

}
