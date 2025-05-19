package codex.renthyl.render;

import codex.renthyl.render.queue.Queueable;

public interface Renderable extends Queueable {

    void update(float tpf);

    void prepare();

    boolean claim(RenderWorker worker);

    void render();

    boolean isRenderingComplete();

    void reset();

    default boolean queueForRender() {
        return true;
    }

}
