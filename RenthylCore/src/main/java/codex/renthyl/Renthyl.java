/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.boost.material.ImmediateShader;
import codex.renthyl.jobs.DefaultJobExecutor;
import codex.renthyl.modules.ControlRenderPass;
import codex.renthyl.modules.geometry.OutputGeometryPass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.renderer.RenderManager;

/**
 * Renthyl is a modular {@link FrameGraph} rendering library for JMonkeyEngine3.
 * <p>
 * To use Renthyl features, first {@link #initialize(com.jme3.app.Application) initialize} Renthyl.
 * <p>
 * <strong>Make contributions or submit bug reports at the Renthyl repository on GitHub:</strong>
 * <em>https://github.com/codex128/Renthyl</em>
 * <p>
 * <strong>To learn how to use Renthyl, visit the Renthyl wiki on GitHub:</strong><br>
 * <em>https://github.com/codex128/Renthyl/wiki/0.-Welcome!</em>
 * <p>
 * <strong>If you have questions about Renthyl, please ask on the JMonkeyEngine forum:</strong><br>
 * <em>https://hub.jmonkeyengine.org/</em>
 * <p>
 * <strong>Consider using Renthyl's official addon library:</strong><br>
 * <em>https://github.com/codex128/RenthylPlus</em>
 * 
 * @author codex
 */
public final class Renthyl {
    
    /**
     * Name of the Renthyl library.
     */
    public static final String LIBRARY_NAME = Renthyl.class.getSimpleName();
    
    private static Renthyl instance;
    
    /**
     * Initializes {@link Renthyl}.
     * <p>
     * This should always be called before using Renthyl functionality
     * in a JMonkeyEngine application.
     * 
     * @param app 
     */
    public static void initialize(Application app) {
        if (isInitialized()) {
            throw new IllegalStateException(LIBRARY_NAME+" has already been initialized.");
        }
        instance = new Renthyl(app);
    }
    
    /**
     * Returns true if Renthyl has been initialized.
     * 
     * @return 
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Throws an exception if Renthyl is not initialized.
     */
    public static void requireInitialized() {
        if (!isInitialized()) {
            throw new IllegalArgumentException(Renthyl.class.getSimpleName()+" has not been initialized.");
        }
    }
    
    /**
     * Returns the main Renthyl instance.
     * 
     * @return 
     */
    public static Renthyl getInstance() {
        return instance;
    }
    
    /**
     * Creates a simple forward-style FrameGraph that emulates
     * JMonkeyEngine's default forward renderer.
     * 
     * @param assetManager
     * @return 
     */
    public static FrameGraph forward(AssetManager assetManager) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        fg.setName("Forward");
        
        fg.add(new ControlRenderPass());
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
        QueueMergePass merge = fg.add(new QueueMergePass());
        OutputGeometryPass out = fg.add(new OutputGeometryPass());
        
        merge.makeInput(enqueue.getMainOutputGroup(), TicketSelector.All, TicketSelector.All);
        out.makeInput(merge, "Result", "Geometry");
        
        return fg;
        
    }
    
    private class AppDestroyState extends BaseAppState {
        
        @Override
        protected void initialize(Application app) {}
        @Override
        protected void cleanup(Application app) {
            defaultExecutor.terminate();
        }
        @Override
        protected void onEnable() {}
        @Override
        protected void onDisable() {}
        
    }
    
    private final DefaultJobExecutor defaultExecutor = new DefaultJobExecutor();
    
    private Renthyl(Application app) {
        
        // attach pipeline
        RenderManager rm = app.getRenderManager();
        rm.registerContext(FrameGraph.CONTEXT_TYPE, new FGPipelineContext(this));
        
        // attach app state to listen for app destruction
        app.getStateManager().attach(new AppDestroyState());
        
        // register loaders and locators
        AssetManager assetManager = app.getAssetManager();
        assetManager.registerLoader(BinaryLoader.class, "fg");
        assetManager.registerLocator("", ImmediateShader.class);
        
    }
    
    /**
     * Gets the default job executor used when no other is specified.
     * 
     * @return 
     */
    public DefaultJobExecutor getBaseExecutor() {
        return defaultExecutor;
    }
    
}
