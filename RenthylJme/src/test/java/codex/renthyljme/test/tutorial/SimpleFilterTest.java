package codex.renthyljme.test.tutorial;

import codex.renthyl.tasks.utils.MapToList;
import codex.renthyljme.JmeFrameGraph;
import codex.renthyljme.filter.FilterChain;
import codex.renthyljme.filter.ports.CrossHatchPass;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyljme.scene.ControlRenderPass;
import codex.renthyljme.scene.GeometryPass;
import codex.renthyljme.scene.OutputPass;
import codex.renthyljme.scene.SceneEnqueuePass;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class SimpleFilterTest extends SimpleApplication {

    public static void main(String[] args) {
        SimpleFilterTest app = new SimpleFilterTest();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        Geometry geometry = new Geometry("cube", new Box(1, 1, 1));
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        geometry.setMaterial(mat);
        rootNode.attachChild(geometry);

        JmeFrameGraph fg = new JmeFrameGraph(assetManager);
        viewPort.setPipeline(fg);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        fg.add(new ControlRenderPass());
        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        MapToList<String, GeometryQueue> orderQueues = new MapToList<>(new String[] {
                RenderQueue.Bucket.Opaque.name(),
                RenderQueue.Bucket.Sky.name(),
                RenderQueue.Bucket.Transparent.name(),
                RenderQueue.Bucket.Gui.name(),
                RenderQueue.Bucket.Translucent.name()});
        GeometryPass render = new GeometryPass(allocator);
        FilterChain filters = new FilterChain();
        filters.add(new CrossHatchPass(assetManager, allocator));
        OutputPass out = fg.addTask(new OutputPass());

        orderQueues.getMap().setUpstream(enqueue.getQueues());
        render.getGeometry().addCollectionSource(orderQueues.getList());
        filters.getSceneColor().setUpstream(render.getOutColor());
        filters.getSceneDepth().setUpstream(render.getOutDepth());
        out.getColor().setUpstream(filters.getFilterResult());
        out.getDepth().setUpstream(render.getOutDepth());

        flyCam.setDragToRotate(true);

    }

}
