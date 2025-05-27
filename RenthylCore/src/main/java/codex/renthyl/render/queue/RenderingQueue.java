package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;
import com.jme3.profile.AppProfiler;

public interface RenderingQueue extends Iterable<Renderable> {

    int stage(Renderable ex);

    void update(float tpf);

    void prepare();

    void render(AppProfiler profiler, int workers);

    void reset();

}
