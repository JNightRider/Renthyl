package codex.renthyl.sockets;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.resources.ResourceWrapper;
import codex.renthyl.render.Renderable;

public class AllocationSocket<T> implements Socket<T> {

    private final Renderable task;
    private final ResourceAllocator allocator;
    private final ResourceDef<T> def;
    private Socket upstream;
    private ResourceWrapper wrapper;
    private T resource;
    private int startingPosition = Integer.MAX_VALUE;
    private int endingPosition = -1;
    private int activeRefs = 0;

    public AllocationSocket(Renderable task, ResourceAllocator allocator, ResourceDef<T> def) {
        this.task = task;
        this.allocator = allocator;
        this.def = def;
    }

    @Override
    public void reference(int position) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(position);
        }
        startingPosition = Math.min(startingPosition, position);
        endingPosition = Math.max(endingPosition, position);
    }

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return task.isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || (upstream.isAvailableToDownstream(queuePosition)
                && upstream.getResourceUsage() == getActiveReferences());
    }

    @Override
    public T acquire() {
        if (resource == null) {
            if (upstream != null) {
                Object val = upstream.acquire();
                if (val != null && def.evaluateResource(val) != null) {
                    wrapper = null;
                    return (resource = def.conformResource(val));
                }
            }
            if (wrapper == null || def.evaluateResource(wrapper.get()) == null || !wrapper.acquire(startingPosition, endingPosition)) {
                wrapper = allocator.allocate(def, startingPosition, endingPosition);
            }
            resource = def.conformResource(wrapper.get());
        }
        return resource;
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
        if (wrapper != null && activeRefs == 0) {
            wrapper.release();
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public void resetSocket() {
        if (activeRefs != 0) {
            throw new IllegalStateException("Socket not fully released.");
        }
        resource = null;
        startingPosition = Integer.MAX_VALUE;
        endingPosition = -1;
    }

    @Override
    public int getResourceUsage() {
        return upstream == null ? activeRefs : Math.max(activeRefs, upstream.getResourceUsage());
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        // queue upstream before queueing owning task
        if (upstream != null) {
            upstream.stage(globals, queue);
        }
        task.stage(globals, queue);
    }

    public void setUpstream(Socket upstream) {
        assertNoActiveReferences();
        this.upstream = upstream;
    }

    public Socket getUpstream() {
        return upstream;
    }

    public ResourceDef<T> getDef() {
        return def;
    }

}
