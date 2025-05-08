/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.tasks.ControlRenderPass;
import codex.renthyl.tasks.OutputPass;
import codex.renthyl.tasks.geometry.GeometryPass;
import codex.renthyl.tasks.geometry.QueueMergePass;
import codex.renthyl.tasks.geometry.SceneEnqueuePass;
import codex.renthyl.tasks.MapToListPass;
import codex.renthyl.resources.ResourceAllocator;
import com.jme3.asset.AssetManager;

/**
 * Renthyl is a modular {@link FrameGraph} rendering library for JMonkeyEngine3.
 * <p>
 * <strong>Make contributions or submit bug reports at the Renthyl repository on
 * <a href="https://github.com/codex128/Renthyl">GitHub</a>.
 * <p>
 * <strong>To learn how to use Renthyl, visit the Renthyl wiki on GitHub:</strong><br>
 * <em>https://github.com/codex128/Renthyl/wiki/0.-Welcome!</em>
 * <p>
 * <strong>If you have questions about Renthyl, please ask on the JMonkeyEngine forum:</strong><br>
 * <em>https://hub.jmonkeyengine.org/</em>
 *
 * @author codex
 */
public final class Renthyl {
    
    /**
     * Creates a simple forward-style FrameGraph that emulates
     * JMonkeyEngine's default forward renderer.
     * 
     * @param assetManager
     * @return 
     */
    public static FrameGraph forward(AssetManager assetManager, ResourceAllocator allocator) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        
        fg.addTask(new ControlRenderPass());
        SceneEnqueuePass enqueue = fg.addTask(SceneEnqueuePass.withLegacyQueues());
        QueueMergePass merge = fg.addTask(new QueueMergePass());
        GeometryPass geometry = fg.addTask(new GeometryPass(allocator));
        OutputPass out = fg.addTask(new OutputPass());

        merge.getQueues().addMapSource(enqueue.getQueues());
        geometry.getGeometry().setUpstream(merge.getResult());

        out.getColor().setUpstream(geometry.getOutColor());
        out.getDepth().setUpstream(geometry.getOutDepth());
        
        return fg;
        
    }
    
}
