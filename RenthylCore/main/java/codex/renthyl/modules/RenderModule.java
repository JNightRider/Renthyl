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
 *
 * @author codex
 */
public interface RenderModule extends Connectable, ResourceUser, Savable {
    
    public void initializeModule(FrameGraph frameGraph);
    public void updateModule(FGRenderContext context, float tpf);
    public void queueModule(FGRenderContext context, ExecutionJobList jobs, int parentThread);
    public void prepareModuleRender(FGRenderContext context); // rename to prepareRender
    public void executeRender(FGRenderContext context);
    public void resetRender(FGRenderContext context);
    public void cleanupModule();
    public void renderingComplete();
    
    public boolean setParent(RenderContainer parent);
    
    public String getName();
    public RenderContainer getParent();
    
    public default void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
    }
    
}
