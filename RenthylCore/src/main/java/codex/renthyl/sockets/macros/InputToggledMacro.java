package codex.renthyl.sockets.macros;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;
import com.jme3.input.controls.ActionListener;

public class InputToggledMacro <T> implements Socket<T>, Macro<T>, ActionListener {

    private final T[] elements;
    private int index = 0;

    @SafeVarargs
    public InputToggledMacro(T... elements) {
        assert elements.length > 0 : "Expected at least one element.";
        this.elements = elements;
    }

    @Override
    public T preview() {
        return elements[index];
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && ++index >= elements.length) {
            index = 0;
        }
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
    public void reference(int queuePosition) {}

    @Override
    public void release(int queuePosition) {}

    @Override
    public int getActiveReferences() {
        return 0;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {}

}
