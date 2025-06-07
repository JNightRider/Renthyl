/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.tasks.scene.ControlRenderPass;
import codex.renthyl.tasks.utils.MapToListPass;
import codex.renthyl.tasks.scene.OutputPass;
import codex.renthyl.tasks.scene.GeometryPass;
import codex.renthyl.tasks.scene.SceneEnqueuePass;
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
        OutputPass out = fg.addTask(new OutputPass());

        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        MapToListPass<String, GeometryQueue> mapToList = new MapToListPass<>(new String[] {
                SceneEnqueuePass.OPAQUE, SceneEnqueuePass.SKY, SceneEnqueuePass.TRANSPARENT, SceneEnqueuePass.GUI, SceneEnqueuePass.TRANSLUCENT});
        GeometryPass geometry = new GeometryPass(allocator);

        mapToList.getMap().setUpstream(enqueue.getQueues());
        geometry.getGeometry().addCollectionSource(mapToList.getList());

        out.getColor().setUpstream(geometry.getOutColor());
        out.getDepth().setUpstream(geometry.getOutDepth());
        
        return fg;
        
    }
    
}
