package codex.renthyl.render;

/**
 * Worker that performs rendering tasks on a thread.
 */
public interface RenderWorker {

    /**
     * Returns an index identifying the thread this worker runs on.
     *
     * @return
     */
    int getThreadIndex();

}
