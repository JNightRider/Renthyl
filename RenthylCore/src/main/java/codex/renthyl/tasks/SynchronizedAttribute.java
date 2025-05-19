package codex.renthyl.tasks;

public class SynchronizedAttribute <T> extends Attribute<T> {

    private static final int PADDING = 5;

    private int[] schedule;
    private int nextPosition = Integer.MAX_VALUE;
    private int lastPosition = -1;

    public SynchronizedAttribute() {
        super();
    }
    public SynchronizedAttribute(T value) {
        super(value);
    }

    @Override
    public void reference(int queuePosition) {
        super.reference(queuePosition);
        if (schedule == null) {
            schedule = new int[queuePosition + PADDING];
        } else if (queuePosition >= schedule.length) {
            int[] temp = new int[queuePosition + PADDING];
            System.arraycopy(schedule, 0, temp, 0, schedule.length);
            schedule = temp;
        }
        schedule[queuePosition]++;
        nextPosition = Math.min(nextPosition, queuePosition);
        lastPosition = Math.max(lastPosition, queuePosition);
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return super.isAvailableToDownstream(queuePosition) && queuePosition == nextPosition;
    }

    @Override
    public void release(int queuePosition) {
        if (queuePosition != nextPosition) {
            throw new IllegalStateException("Release of synchronized socket is out of order.");
        }
        super.release(queuePosition);
        schedule[nextPosition]--;
        while (nextPosition <= lastPosition && schedule[nextPosition] <= 0) {
            nextPosition++;
        }
    }

    @Override
    public void resetSocket() {
        if (nextPosition <= lastPosition) {
            throw new IllegalStateException("Not all scheduled references were released.");
        }
        nextPosition = Integer.MAX_VALUE;
        lastPosition = -1;
    }

}
