package codex.renthyl.newresources;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceException;

public class AllocationSocket<T> extends ModifyingSocket<T> {

    private final ResourceAllocator<AllocatedResource> allocator;
    private final ResourceDef<T> def;
    private AllocatedResource allocated;
    private T resource;
    private int startingPosition = Integer.MAX_VALUE;
    private int endingPosition = -1;

    public AllocationSocket(RenderTask task, ResourceAllocator allocator, ResourceDef<T> def) {
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
            // TODO: address concurrent issues with acquiring resources
            if (allocated == null || def.evaluateResource(allocated.get()) == null
                    || !allocated.acquire(startingPosition, endingPosition)) {
                allocated = allocator.allocate(def, startingPosition, endingPosition);
            }
            try {
                resource = def.conformResource(allocated.get());
            } catch (ResourceException e) {
                throw new RuntimeException("Failed to conform resource.", e);
            }
        }
        return resource;
    }

    @Override
    public void release() {
        super.release();
        if (allocated != null && activeRefs <= 0) {
            allocated.release();
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
