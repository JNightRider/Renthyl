/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.jobs.ExecutionJobList;
import codex.renthyl.resources.ResourceUser;
import com.jme3.export.Savable;
import java.util.function.Consumer;

/**
 * Rendering module or task that can be attached to a {@link FrameGraph}
 * and executed.
 * 
 * @author codex
 */
public interface RenderModule extends Connectable, ResourceUser, Savable {
    
    /**
     * Called when the module is first attached to a {@link FrameGraph}.
     * <p>
     * An exception is thrown if this module is currently attached to
     * a FrameGraph.
     * 
     * @param frameGraph 
     */
    public void initializeModule(FrameGraph frameGraph);
    
    /**
     * Called each time the FrameGraph is rendered.
     * 
     * @param context
     * @param tpf 
     */
    public void updateModule(FGRenderContext context, float tpf);
    
    /**
     * Adds the module to an execution job.
     * 
     * @param context
     * @param jobs
     * @param parentThread 
     */
    public void queueModule(FGRenderContext context, ExecutionJobList jobs, int parentThread);
    
    /**
     * Prepares the module for execution.
     * <p>
     * A call to this method is not always followed by an
     * {@link #executeRender(codex.renthyl.FGRenderContext) execute}
     * call due to culling, but is always followed by a
     * {@link #resetRender(codex.renthyl.FGRenderContext) reset} call.
     * 
     * @param context 
     */
    public void prepareRender(FGRenderContext context);
    
    /**
     * Executes this module.
     * <p>
     * A call to this method is always followed by a
     * {@link #resetRender(codex.renthyl.FGRenderContext) reset} call.
     * 
     * @param context 
     */
    public void executeRender(FGRenderContext context);
    
    /**
     * Resets the module from execution.
     * <p>
     * A {@link #prepareRender(codex.renthyl.FGRenderContext) prepare} or
     * {@link #executeRender(codex.renthyl.FGRenderContext) execute} call is
     * always followed by a call to this method.
     * 
     * @param context 
     */
    public void resetRender(FGRenderContext context);
    
    /**
     * Called when the module is detached from a {@link FrameGraph}.
     */
    public void cleanupModule();
    
    /**
     * Called when all rendering operations have been completed in a frame
     * in which the {@link FrameGraph} this module is attached to
     * participated in.
     */
    public void renderingComplete();
    
    /**
     * Directly sets the parent container of this module.
     * <p>
     * This should be called by {@link RenderContainer} implementations
     * <em>only</em>.
     * 
     * @param parent parent container to attach to
     * @return true if this module accepts attachement to {@code parent}
     */
    public boolean setParent(RenderContainer parent);
    
    /**
     * Gets the name of this module.
     * 
     * @return 
     */
    public String getName();
    
    /**
     * Gets the parent of this module.
     * 
     * @return 
     */
    public RenderContainer getParent();
    
    /**
     * Performs a depth-first traversal of this module and child modules,
     * calling the Consumer for each module traversed.
     * 
     * @param traverser Consumer to call (not null)
     */
    public default void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
    }
    
}
