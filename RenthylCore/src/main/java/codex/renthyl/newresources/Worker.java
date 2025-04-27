package codex.renthyl.newresources;

public interface Worker extends Runnable {

    int getThreadIndex();

    boolean run(Executable ex);

}
