package codex.renthyl.sockets;

import codex.renthyl.render.Queueable;
import codex.renthyl.render.Referenceable;

import java.util.Objects;

public interface Socket <T> extends Queueable, Referenceable {

    void update(float tpf);

    boolean isAvailableToDownstream(int queuePosition);

    boolean isUpstreamAvailable(int queuePosition);

    T acquire();

    void reset();

    int getResourceUsage();

    default T acquire(T orElse) {
        T resource = acquire();
        return resource != null ? resource : orElse;
    }

    default T acquireOrThrow() {
        return acquireOrThrow("Unable to acquire.");
    }

    default T acquireOrThrow(String message) {
        return Objects.requireNonNull(acquire(), message);
    }

}
