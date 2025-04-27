package codex.renthyl.newresources;

public interface ExecutionQueue extends Iterable<Executable> {

    int add(Executable ex);

    void executeNext(Worker worker);

    boolean containsAtPosition(Executable ex, int position);

    boolean isEmpty();

    void clear();

}
