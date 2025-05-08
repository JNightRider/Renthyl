package codex.renthyl.sockets;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.resources.ResourceWrapper;
import codex.renthyl.render.Renderable;
import codex.renthyl.resources.ResourceException;

public class AllocationSocket<T> extends ModifyingSocket<T> {

    private final ResourceAllocator allocator;
    private final ResourceDef<T> def;
    private ResourceWrapper wrapper;
    private T resource;
    private int startingPosition = Integer.MAX_VALUE;
    private int endingPosition = -1;

    public AllocationSocket(Renderable task, ResourceAllocator allocator, ResourceDef<T> def) {
        super(task);
        this.allocator = allocator;
        this.def = def;
    }

    @Override
    public void reference(int position) {
        super.reference(position);
        startingPosition = Math.min(startingPosition, position);
        endingPosition = Math.max(endingPosition, position);
    }

    @Override
    public T acquire() {
        if (upstream != null) {
            return upstream.acquire();
        }
        if (resource == null) {
            if (wrapper == null || def.evaluateResource(wrapper.get()) == null
                    || !wrapper.acquire(startingPosition, endingPosition)) {
                wrapper = allocator.allocate(def, startingPosition, endingPosition);
            }
            try {
                resource = def.conformResource(wrapper.get());
            } catch (ResourceException e) {
                throw new RuntimeException("Failed to conform resource.", e);
            }
        }
        return resource;
    }

    @Override
    public void release() {
        super.release();
        if (wrapper != null && activeRefs <= 0) {
            wrapper.release();
            resource = null;
        }
    }

    @Override
    public void reset() {
        super.reset();
        startingPosition = Integer.MAX_VALUE;
        endingPosition = -1;
    }

    public ResourceDef<T> getDef() {
        return def;
    }

}
