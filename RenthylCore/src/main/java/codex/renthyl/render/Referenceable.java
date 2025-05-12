package codex.renthyl.render;

public interface Referenceable {

    void reference(int queuePosition);

    void release(int queuePosition);

    int getActiveReferences();

    default void assertNoActiveReferences() {
        if (getActiveReferences() > 0) {
            throw new IllegalStateException("Cannot have active references during this operation.");
        }
    }

}
