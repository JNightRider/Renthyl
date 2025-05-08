package codex.renthyl.render;

import codex.renthyl.FGRenderContext;

public interface RenderingQueue extends Iterable<Renderable> {

    int add(Renderable ex);

    void update(float tpf);

    void prepare();

    void render(int workers, FGRenderContext context);

    void reset();

}
