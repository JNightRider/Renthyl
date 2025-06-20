package codex.renthyljme.resources;

import codex.renthyl.resources.ShortTermAllocator;
import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.RenderManager;

/**
 * Basic resource allocator.
 *
 * <p>Resources that go unused for several render frames are disposed. The exact number of frames before this happens
 * can be configured. All resources are disposed on cleanup; either when the application closes or this state is
 * detached from the {@link com.jme3.app.state.AppStateManager}.</p>
 *
 * @author codex
 */
public class ResourceAllocationState extends ShortTermAllocator implements AppState {

    private boolean initialized = false;
    private boolean enabled = false;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        initialized = true;
        enabled = true;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void setEnabled(boolean active) {
        this.enabled = active;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void stateAttached(AppStateManager stateManager) {

    }

    @Override
    public void stateDetached(AppStateManager stateManager) {

    }

    @Override
    public void update(float tpf) {
        flush();
    }

    @Override
    public void render(RenderManager rm) {

    }

    @Override
    public void postRender() {

    }

    @Override
    public void cleanup() {
        clear();
    }

}
