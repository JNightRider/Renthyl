package codex.renthyl.tasks.utils;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyl.tasks.Frame;

import java.util.ArrayList;
import java.util.List;

/**
 * Socket which chooses from a list of possible upstream sockets using an
 * integer index to treat as upstream.
 *
 * <p>Sockets that are not chosen as upstream are not staged, referenced,
 * acquired, or released, so Multiplexor does not cause those tasks to run.
 * The index used is attained during staging by an {@link ArgumentMacro}.</p>
 *
 * @param <T>
 * @author codex
 */
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

    /**
     * Returns the index to use next.
     *
     * @param index previous index
     * @return next index
     */
    protected int getNextIndex(int index) {
        return indexMacro.preview();
    }

    /**
     * Returns true if the current upstream index is not a valid index
     * for the number of possibilities.
     *
     * @return
     */
    protected boolean isNullIndex() {
        return index < 0 || index >= upstream.size();
    }

    /**
     * Adds the socket to the end of the possible upstream socket list.
     *
     * @param upstream
     * @return
     */
    public int addUpstream(Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.add(upstream);
        return this.upstream.size() - 1;
    }

    /**
     * Adds the socket at the index of the possible upstream socket list.
     *
     * @param i
     * @param upstream
     */
    public void addUpstream(int i, Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.add(i, upstream);
    }

    /**
     * Sets the element at the index of the possible upstream socket list.
     *
     * @param i
     * @param upstream
     */
    public void setUpstream(int i, Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream.set(i, upstream);
    }

    /**
     * Removes the socket from the possible upstream socket list.
     *
     * @param upstream
     */
    public void removeUpstream(Socket upstream) {
        assertUnqueued();
        this.upstream.remove(upstream);
    }

    /**
     * Gets macro determining the upstream index.
     *
     * @return
     */
    public ArgumentMacro<Integer> getIndex() {
        return indexMacro;
    }

    /**
     * Gets the number of possible upstream sockets.
     *
     * @return
     */
    public int size() {
        return upstream.size();
    }

}
