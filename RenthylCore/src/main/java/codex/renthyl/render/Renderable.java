package codex.renthyl.render;

import codex.renthyl.FGRenderContext;

public interface Renderable extends Queueable {

    void update(float tpf);

    void prepare();

    boolean claim(RenderWorker worker);

    void render(FGRenderContext context);

    boolean isRenderingComplete();

    void reset();

}
