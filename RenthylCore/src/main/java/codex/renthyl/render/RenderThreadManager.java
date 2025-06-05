package codex.renthyl.render;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic thread executor that uses an {@link ExecutorService} to manage
 * worker threads.
 *
 * <p>All threads are {@link ExecutorService#shutdown()} on cleanup.</p>
 *
 * @author codex
 */
public class RenderThreadManager extends BaseAppState implements Executor {

    private final ExecutorService service;

    public RenderThreadManager() {
        this(Executors.newCachedThreadPool());
    }
    public RenderThreadManager(ExecutorService service) {
        this.service = service;
    }

    @Override
    protected void initialize(Application app) {}

    @Override
    protected void cleanup(Application app) {
        service.shutdown();
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    public void execute(Runnable command) {
        service.execute(command);
    }

}
