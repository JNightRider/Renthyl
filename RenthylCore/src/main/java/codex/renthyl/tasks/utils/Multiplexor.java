package codex.renthyl.tasks.utils;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyl.tasks.Frame;

import java.util.ArrayList;
import java.util.List;

public class Multiplexor <T> extends Frame implements Socket<T> {

    private final List<Socket<? extends T>> upstream = new ArrayList<>();
    private final ArgumentMacro<Integer> indexMacro = new ArgumentMacro<>();
    private int activeRefs = 0;
    private int index = 0;

    public Multiplexor() {
        this(0);
    }
    public Multiplexor(int index) {
        indexMacro.setValue(index);
        addSocket(indexMacro);
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (position < QUEUING) {
            index = getNextIndex(index);
            if (!isNullIndex()) {
                upstream.get(index).stage(globals, queue);
            }
        }
        super.stage(globals, queue);
    }

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return isRenderingComplete() && isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return isNullIndex() || upstream.get(index).isAvailableToDownstream(queuePosition);
    }

    @Override
    public T acquire() {
        return !isNullIndex() ? upstream.get(index).acquire() : null;
    }

    @Override
    public void resetSocket() {
        if (activeRefs > 0) {
            throw new IllegalStateException("More references than releases.");
        }
    }

    @Override
    public int getResourceUsage() {
        return isNullIndex() ? activeRefs : Math.max(activeRefs, upstream.get(index).getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (!isNullIndex()) {
            upstream.get(index).reference(queuePosition);
        }
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (!isNullIndex()) {
            upstream.get(index).release(queuePosition);
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    protected int getNextIndex(int index) {
        return indexMacro.preview();
    }

    public int addUpstream(Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.add(upstream);
        return this.upstream.size() - 1;
    }

    public void addUpstream(int i, Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.add(i, upstream);
    }

    public void setUpstream(int i, Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.set(i, upstream);
    }

    public void removeUpstream(Socket upstream) {
        assertUnqueued();
        this.upstream.remove(upstream);
    }

    public ArgumentMacro<Integer> getIndex() {
        return indexMacro;
    }

    public int size() {
        return upstream.size();
    }

    public boolean isNullIndex() {
        return index < 0 || index >= upstream.size();
    }

}
