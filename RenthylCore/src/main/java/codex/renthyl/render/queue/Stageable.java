package codex.renthyl.render.queue;

import codex.renthyl.GlobalAttributes;

public interface Stageable {

    void stage(GlobalAttributes globals, RenderingQueue queue);

}
