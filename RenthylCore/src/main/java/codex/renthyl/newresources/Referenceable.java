package codex.renthyl.newresources;

public interface Referenceable {

    void reference(int queuePosition);

    void release();

    int getActiveReferences();

}
