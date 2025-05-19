package codex.renthyl.sockets;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.render.Renderable;
import codex.renthyl.resources.ResourceAllocator;

import java.util.HashMap;
import java.util.Map;

public class AllocationSocketMap<K, D extends ResourceDef<R>, R> extends SocketMap<K, AllocationSocket<R>, R> {

    private final Map<K, D> defs = new HashMap<>();

    public AllocationSocketMap(Renderable task) {
        super(task);
    }

    public AllocationSocketMap(Renderable task, Map<K, R> resourceMap) {
        super(task, resourceMap);
    }

    public AllocationSocket<R> put(K key, ResourceAllocator allocator, D def) {
        AllocationSocket<R> s = new AllocationSocket<>(task, allocator, def);
        put(key, s);
        defs.put(key, def);
        return s;
    }

    public D getDef(K key) {
        return defs.get(key);
    }

    public Map<K, D> getDefs() {
        return defs;
    }

}
