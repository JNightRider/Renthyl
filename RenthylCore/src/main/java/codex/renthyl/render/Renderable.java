package codex.renthyl.render;

public interface Renderable extends Queueable {

    void update(float tpf);

    void prepare();

    boolean claim(RenderWorker worker);

    void render();

    boolean isRenderingComplete();

    void reset();

}
