package codex.renthyl.render;

public interface RenderingQueue extends Iterable<Renderable> {

    int add(Renderable ex);

    void update(float tpf);

    void prepare();

    void render(int workers);

    void reset();

}
