package codex.renthyl.sockets.macros;

import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;

public class ArgumentMacro<T> implements Socket<T>, Macro<T> {

    private Macro<? extends T> upstream;
    private T value;

    public ArgumentMacro() {}
    public ArgumentMacro(T value) {
        this.value = value;
    }

    @Override
    public T preview() {
        return upstream != null ? upstream.preview() : value;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return true;
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return true;
    }

    @Override
    public T acquire() {
        return preview();
    }

    @Override
    public void resetSocket() {}

    @Override
    public int getResourceUsage() {
        return 0;
    }

    @Override
    public void stage(RenderingQueue queue) {}

    @Override
    public void reference(int queuePosition) {}

    @Override
    public void release(int queuePosition) {}

    @Override
    public int getActiveReferences() {
        return 0;
    }

    public void setUpstream(Macro<? extends T> upstream) {
        this.upstream = upstream;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public Macro<? extends T> getUpstream() {
        return upstream;
    }

    public T getValue() {
        return value;
    }

}
