package codex.renthyl.resources;

public interface ResourceWrapper <T> {

    boolean acquire(int start, int end);

    T get();

    void release();

    void reserve(int position);

    boolean isAvailable();

    boolean isReserved(int start, int end);

    default boolean isAvailable(int start, int end) {
        return isAvailable() && !isReserved(start, end);
    }

}
