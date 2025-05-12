/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.examples;

import codex.renthyl.Renthyl;
import codex.renthyl.resources.ResourceAllocationState;
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
public class TestForward extends SimpleApplication {
    
    public static void main(String[] args) {
        new TestForward().start();
    }
    
    @Override
    public void simpleInitApp() {
        
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

        // create a resource allocator to handle resources
        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);
        
        // create the framegraph
        viewPort.setPipeline(Renthyl.forward(assetManager, allocator));
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        
        // Force the camera to always look at the center of the world, so that
        // we don't accidentally miss the cube entirely and confuse that for a bug.
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        
    }
    
}

