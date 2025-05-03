package codex.renthyl.newresources;

import codex.renthyl.FGRenderContext;

public interface RenderingQueue extends Iterable<Renderable> {

    int add(Renderable ex);

    void render(int workers, FGRenderContext context);

    void flush();

}
