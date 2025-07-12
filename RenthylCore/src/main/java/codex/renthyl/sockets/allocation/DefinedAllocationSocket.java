package codex.renthyl.sockets.allocation;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.render.Renderable;
import codex.renthyl.resources.ResourceAllocator;

/**
 * Allocation socket that contains a parameterized resource definition.
 *
 * @param <D>
 * @param <T>
 */
public class DefinedAllocationSocket <D extends ResourceDef<T>, T> extends AllocationSocket<T> {

    private final D def;

    public DefinedAllocationSocket(Renderable task, ResourceAllocator allocator, D def) {
        super(task, allocator, def);
        this.def = def;
    }

    @Override
    public D getDef() {
        return def;
    }

}
