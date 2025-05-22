package codex.renthyl.render;

import codex.renthyl.render.queue.Stageable;

public interface Renderable extends Stageable {

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
