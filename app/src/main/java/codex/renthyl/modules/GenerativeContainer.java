/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.modules.protocol.FilterProtocol;
import codex.renthyl.resources.tickets.TicketSignature;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class GenerativeContainer <T extends RenderModule> extends RenderContainer<T> {
    
    @Override
    public <R extends T> R add(R module, int index) {
        super.add(module, index);
        memberAdded(module, index);
        return module;
    }
    @Override
    public T remove(int index) {
        T m = super.remove(index);
        if (m != null) {
            memberRemoved(index);
        }
        return m;
    }
    
    protected abstract void memberAdded(T module, int index);
    protected abstract void memberRemoved(int index);
    
    public static class TestFilterContainer extends GenerativeContainer<FilterProtocol> implements FilterProtocol {

        @Override
        protected void memberAdded(FilterProtocol module, int index) {
            if (index > 0) {
                FilterProtocol prev = queue.get(index-1);
                connectMembers(prev, module);
            } else {
                connectToContainerInput(module);
            }
            if (index < queue.size()-1) {
                FilterProtocol next = queue.get(index+1);
                connectMembers(module, next);
            } else {
                connectToContainerOutput(module);
            }
            module.makeInput(this, getRenderedSceneDepth(), module.getRenderedSceneDepth());
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
        
        protected void connectMembers(FilterProtocol source, FilterProtocol target) {
            target.makeInput(source, source.getFilteredResult(), target.getRenderedSceneColor());
        }
        protected void connectToContainerInput(FilterProtocol target) {
            target.makeInput(this, getRenderedSceneColor(), target.getRenderedSceneColor());
        }
        protected void connectToContainerOutput(FilterProtocol source) {
            makeInput(source, source.getFilteredResult(), getFilteredResult());
        }
        protected void connectContainer() {
            makeInput(this, getRenderedSceneColor(), getFilteredResult());
        }

        @Override
        public TicketSignature getRenderedSceneColor() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public TicketSignature getRenderedSceneDepth() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }

        @Override
        public TicketSignature getFilteredResult() {
            throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
        }
        
    }
    
}
