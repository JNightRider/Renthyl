package codex.renthyl.resources;

public interface Ticket <T, R> {

    void setIndex(T index);

    T getIndex();

    static <T, R> Ticket<T, R> ticket(T index) {
        return new TicketImpl<>(index);
    }

    class TicketImpl <T, R> implements Ticket<T, R> {

        private T index;

        public TicketImpl() {}
        public TicketImpl(T index) {
            this.index = index;
        }

        @Override
        public void setIndex(T index) {
            this.index = index;
        }
        @Override
        public T getIndex() {
            return index;
        }

    }

}
