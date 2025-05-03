package codex.renthyl.newresources;

public interface Socket <T> extends Queueable, Referenceable {

    void setUpstream(Socket<T> upstream);

    Socket<T> getUpstream();

    boolean isAvailableToDownstream();

    boolean isUpstreamAvailable();

    T acquire();

    void reset();

    static <T> TransitSocket<T> create(Renderable task) {
        return new TransitSocket<>(task);
    }

}
