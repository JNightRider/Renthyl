package codex.renthyl.tasks;

import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyl.sockets.Socket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapToListPass <K, R> extends AbstractTask {

    private final K[] order;
    private final TransitiveSocket<Map<K, R>> map = new TransitiveSocket<>(this);
    private final ValueSocket<List<R>> list = new ValueSocket<>(this);

    public MapToListPass() {
        this((K[])null);
    }
    public MapToListPass(K[] order) {
        this(order, new ArrayList<>());
    }
    public MapToListPass(List<R> list) {
        this(null, list);
    }
    public MapToListPass(K[] order, List<R> list) {
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

    public PointerSocket<Map<K, R>> getMap() {
        return map;
    }

    public Socket<List<R>> getList() {
        return list;
    }

}
