/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.FrameGraph;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class SequenceContainer <T extends RenderModule> extends GenerativeContainer<T> {
    
    @Override
    public void initializeModule(FrameGraph frameGraph) {
        super.initializeModule(frameGraph);
        connectContainer();
    }
    @Override
    protected void memberAdded(T module, int index) {
        if (index > 0) {
            T prev = queue.get(index-1);
            connectMembers(prev, module);
        } else {
            connectToContainerInput(module);
        }
        if (index < queue.size()-1) {
            T next = queue.get(index+1);
            connectMembers(module, next);
        } else {
            connectToContainerOutput(module);
        }
    }
    @Override
    protected void memberRemoved(int index) {
        if (queue.isEmpty()) {
            connectContainer();
            return;
        }
        if (index == 0) {
            connectToContainerInput(queue.get(index));
        }
        if (index == queue.size()) {
            connectToContainerOutput(queue.get(index-1));
        }
        if (queue.size() >= 2 && index > 0 && index < queue.size()) {
            connectMembers(queue.get(index-1), queue.get(index));
        }
    }
    
    protected abstract void connectMembers(T source, T target);
    protected abstract void connectToContainerInput(T target);
    protected abstract void connectToContainerOutput(T source);
    protected abstract void connectContainer();
    
}
