/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.util;

import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.Junction;
import com.jme3.input.InputManager;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.renderer.ViewPort;

/**
 *
 * @author codex
 */
public class IndexSwitch implements GraphSource<Integer>, ActionListener {
    
    private static long nextId = 0;
    
    private int index, limit;

    public IndexSwitch(InputManager im, Trigger trigger) {
        this(im, trigger, 0, 1);
    }
    public IndexSwitch(InputManager im, Trigger trigger, int index) {
        this(im, trigger, index, index + 1);
    }
    public IndexSwitch(InputManager im, Trigger trigger, int index, int limit) {
        this.index = index;
        this.limit = limit;
        setupInput(im, trigger);
    }
    
    private void setupInput(InputManager im, Trigger trigger) {
        String mapping = getClass().getName() + "" + (nextId++);
        im.addMapping(mapping, trigger);
        im.addListener(this, mapping);
    }
    
    public void setJunction(Junction junct) {
        junct.setIndexSource(this);
        limit = junct.getLength();
    }
    public void cleanupInput(InputManager im) {
        im.removeListener(this);
    }
    
    @Override
    public Integer getGraphValue(FrameGraph frameGraph, ViewPort viewPort) {
        return index;
    }
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (isPressed && ++index >= limit) {
            index = 0;
        }
    }
    
}
