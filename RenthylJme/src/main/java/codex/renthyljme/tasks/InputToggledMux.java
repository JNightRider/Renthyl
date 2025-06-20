package codex.renthyljme.tasks;

import codex.renthyl.tasks.utils.Multiplexor;
import com.jme3.input.controls.ActionListener;

/**
 * Multiplexor whose index is incremented on an input action.
 *
 * @param <T>
 * @author codex
 */
public class InputToggledMux <T> extends Multiplexor<T> implements ActionListener {

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            int i = getIndex().getValue();
            if (++i >= size()) i = 0;
            getIndex().setValue(i);
        }
    }

}
