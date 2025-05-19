package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;

public interface RenderingQueue extends Iterable<Renderable> {

    int stage(Renderable ex);

    void update(float tpf);

    void prepare();

    void render(int workers);

    void reset();

}
