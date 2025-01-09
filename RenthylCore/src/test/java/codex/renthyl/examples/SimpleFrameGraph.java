/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.examples;

import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.modules.ControlRenderPass;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.geometry.GeometryPass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * An example demonstrating how to create a simple FrameGraph that is
 * functionally the same as JME's default forward renderer.
 * 
 * @author codex
 */
public class SimpleFrameGraph extends SimpleApplication {
    
    public static void main(String[] args) {
        new SimpleFrameGraph().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        /*********
         * Setup *
         *********/
        
        // Create and attach a cube to the scene for the FrameGraph to render.
        Geometry cube = new Geometry("Cube", new Box(1, 1, 1));
        Material cubeMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        cubeMat.setColor("Color", ColorRGBA.Blue);
        cube.setMaterial(cubeMat);
        rootNode.attachChild(cube);
        
        // Make the viewport's background not black so we know if the FrameGraph
        // is indeed rendering to the screen at all.
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        
        // Increase the camera's speed.
        flyCam.setMoveSpeed(10f);
        
        /**************
         * FrameGraph *
         **************/
        
        // Before anything else, initialize Renthyl.
        Renthyl.initialize(this);
        
        // Declare a new FrameGraph and attach it to the main viewport.
        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        // Add a pass that will call render() for every control in the viewport's scenes.
        // This is not crucial, but without it some things may not behave correctly, such
        // as particles.
        fg.add(new ControlRenderPass());
        
        // Add a pass that will put all geometries in the viewport's scenes into queues
        // that will make rendering a lot more efficient. Using "withLegacyQueues" creates an
        // instance with the same queues that JME would normally have (opaque, sky, 
        // transparent, gui, and translucent).
        SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withLegacyQueues());
        
        // Note: you can alternatively use your own queues like so:
            // SceneEnqueuePass enqueue = new SceneEnqueuePass();
            // enqueue.add("MyQueue", new GeometryQueue());
            // fg.add(enqueue);
        // Doing so is useful if you ever want to render a set of geometries
        // differently from other geometries.
        
        // Add a pass that will merge all the queues created by the previous pass into
        // a single queue that can easily be passed around. We pass "5" as the argument
        // because we want to merge 5 queues into one.
        QueueMergePass merge = fg.add(new QueueMergePass());
        
        // Add a pass that will render all geometries in a queue to a color texture and
        // a depth texture.
        GeometryPass geometry = fg.add(new GeometryPass());
        
        // Finally, add a pass that will display the color and depth textures created
        // above on the screen.
        OutputPass out = fg.add(new OutputPass());
        
        // Connect the enqueue pass to the merge pass. In this case, we're telling
        // the merge pass to merge the opaque, sky, transparent, gui, and translucent
        // queues created by the enqueue pass into a single queue.
        merge.makeInput(enqueue.getMainOutputGroup(),
                TicketSelector.names("Opaque", "Sky", "Transparent", "Gui", "Translucent"), TicketSelector.All);
        
        // Connect the merge pass to the geometry pass. This way, the geometry pass
        // will render from the merged queue created by the merge pass.
        geometry.makeInput(merge, "Result", "Geometry");
        
        // Finally, connect the geometry pass to the output pass. This way, the output
        // pass will render the color texture created by the geometry pass to the screen.
        out.makeInput(geometry, "Color", "Color");
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        
        // Force the camera to always look at the center of the world, so that
        // we don't accidentally miss the cube entirely and confuse that for a bug.
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        
    }
    
}

