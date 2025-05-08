package codex.renthyl.sockets;

import codex.renthyl.render.Queueable;
import codex.renthyl.render.Referenceable;

public interface Socket <T> extends Queueable, Referenceable {

    void update(float tpf);

    boolean isAvailableToDownstream();

    boolean isUpstreamAvailable();

    T acquire();

    void reset();

    int getResourceUsage();

    default T acquire(T orElse) {
        T resource = acquire();
        return resource != null ? resource : orElse;
    }

}
