package codex.renthyl.newresources;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BasicExecutionQueue implements ExecutionQueue {

    private final ConcurrentLinkedQueue<Executable> queue = new ConcurrentLinkedQueue<>();

    @Override
    public int add(Executable ex) {
        queue.add(ex);
        return queue.size() - 1;
    }

    @Override
    public void executeNext(Worker worker) {
        for (Iterator<Executable> it = queue.iterator(); it.hasNext();) {
            Executable ex = it.next();
            if (worker.run(ex)) {
                it.remove();
                break;
            }
        }
    }

    @Override
    public boolean containsAtPosition(Executable ex, int position) {
        // TODO: this is not very efficient
        return queue.contains(ex);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public Iterator<Executable> iterator() {
        return queue.iterator();
    }

}
