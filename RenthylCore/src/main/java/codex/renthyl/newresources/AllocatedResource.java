package codex.renthyl.newresources;

import java.util.BitSet;

public class AllocatedResource <T> {

    private final long id;
    private final T resource;
    private boolean acquired = false;
    private final BitSet reservedPositions = new BitSet();

    public AllocatedResource(long id, T resource) {
        this.id = id;
        this.resource = resource;
    }

    public boolean acquire(int start, int end) {
        if (acquired || isReserved(start, end)) {
            return false;
        }
        return (acquired = true);
    }

    public T get() {
        return resource;
    }

    public void release() {
        if (!acquired) {
            throw new IllegalStateException("Resource was not acquired.");
        }
        acquired = false;
    }

    public void reserve(int position) {
        reservedPositions.set(position);
    }

    public boolean isReserved(int start, int end) {
        // does not detect reservations that are on start
        for (start++; start <= end; start++) {
            if (reservedPositions.get(start)) {
                return true;
            }
        }
        return false;
    }

    protected void clearReservations() {
        reservedPositions.clear();
    }

}
