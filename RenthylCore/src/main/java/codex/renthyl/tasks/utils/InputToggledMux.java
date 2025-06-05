package codex.renthyl.tasks.utils;

import com.jme3.input.controls.ActionListener;

/**
 * Multiplexor whose index is incremented on input action.
 *
 * @param <T>
 * @author codex
 */
public class InputToggledMux <T> extends Multiplexor<T> implements ActionListener {

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed) {
            int i = getIndex().preview();
            if (++i >= size()) i = 0;
            getIndex().setValue(i);
        }
    }

}
