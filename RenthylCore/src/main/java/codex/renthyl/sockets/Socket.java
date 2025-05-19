package codex.renthyl.sockets;

import codex.renthyl.render.queue.Queueable;
import codex.renthyl.render.Referenceable;
import com.jme3.material.Material;

import java.util.Objects;

public interface Socket <T> extends Queueable, Referenceable {

    void update(float tpf);

    boolean isAvailableToDownstream(int queuePosition);

    boolean isUpstreamAvailable(int queuePosition);

    T acquire();

    void resetSocket();

    int getResourceUsage();

    default T acquire(T orElse) {
        T resource = acquire();
        return resource != null ? resource : orElse;
    }

    default T acquireOrThrow() {
        return acquireOrThrow("Unable to acquireType.");
    }

    default T acquireOrThrow(String message) {
        return Objects.requireNonNull(acquire(), message);
    }

    default <R> R acquireType(Class<R> type) {
        T v = acquire();
        if (v == null) {
            return null;
        }
        if (!type.isAssignableFrom(v.getClass())) {
            return (R)v;
        }
        throw new ClassCastException("Cannot cast " + v.getClass() + " resource to " + type);
    }

    default T acquireToMaterial(Material material, String param) {
        T v = acquire();
        if (v != null) {
            material.setParam(param, v);
        } else {
            material.clearParam(param);
        }
        return v;
    }

}
