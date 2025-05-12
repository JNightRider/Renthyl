package codex.renthyl.tasks;

import codex.renthyl.sockets.DynamicSocketList;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.ValueSocket;

public class Multiplexor <T> extends AbstractTask {

    private final DynamicSocketList<TransitiveSocket<T>, T> inputs = new DynamicSocketList<>(this, () -> new TransitiveSocket<>(this));
    private final ValueSocket<T> output = new ValueSocket<>(this);
    private int index = -1;

    public Multiplexor() {
        this(null);
    }
    public Multiplexor(T defaultValue) {
        addSockets(inputs, output);
        output.setValue(defaultValue);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        setIndex(index);
    }

    @Override
    protected void renderTask() {}

    public void setIndex(int index) {
        this.index = index;
        if (this.index >= 0) {
            TransitiveSocket<T> in = inputs.get(this.index);
            if (in.getUpstream() != null) {
                output.setUpstream(in);
                return;
            }
        }
        output.setUpstream(null);
    }

    public void setDefaultValue(T value) {
        output.setValue(value);
    }

    public DynamicSocketList<?, T> getInputs() {
        return inputs;
    }

    public Socket<T> getOutput() {
        return output;
    }

}
