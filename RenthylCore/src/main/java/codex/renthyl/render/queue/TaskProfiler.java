package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;

/**
 * Profiles tasks for the application as post-processor pre-frame steps.
 */
public class TaskProfiler implements RenderingListener {

    private final AppProfiler profiler;

    public TaskProfiler(AppProfiler profiler) {
        this.profiler = profiler;
    }

    @Override
    public void onTaskBegin(Renderable task) {
        profiler.spStep(SpStep.ProcPreFrame, task.toString());
    }

    @Override
    public void onTaskEnd(Renderable task) {}

    public AppProfiler getProfiler() {
        return profiler;
    }

}
