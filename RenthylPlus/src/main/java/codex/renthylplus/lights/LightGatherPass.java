/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.lights;

import codex.renthyl.sockets.ValueSocket;
import codex.renthyljme.tasks.RasterTask;
import com.jme3.light.Light;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author codex
 */
public class LightGatherPass extends RasterTask {

    private final ValueSocket<Collection<Light>> lights = new ValueSocket<>(this, new ArrayList<>());

    public LightGatherPass() {
        addSockets(lights);
    }

    @Override
    protected void renderTask() {
        lights.getValue().clear();
        for (Spatial scene : context.getViewPort().getScenes()) {
            for (Spatial spatial : new SceneGraphIterator(scene)) {
                for (Light l : spatial.getLocalLightList()) {
                    lights.getValue().add(l);
                }
            }
        }
    }

    public ValueSocket<Collection<Light>> getLights() {
        return lights;
    }

}
