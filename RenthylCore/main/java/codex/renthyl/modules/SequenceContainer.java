/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.resources.tickets.TicketSelector;

/**
 *
 * @author codex
 * @param <T>
 */
public class SequenceContainer <T extends RenderModule> extends RenderContainer<T> {
    
    private final TicketSelector localSource, localTarget;
    private final TicketSelector globalSource, globalTarget;

    public SequenceContainer(TicketSelector localSource, TicketSelector localTarget, TicketSelector globalSource, TicketSelector globalTarget) {
        this.localSource = localSource;
        this.localTarget = localTarget;
        this.globalSource = globalSource;
        this.globalTarget = globalTarget;
    }
    
    @Override
    public <R extends T> R add(R module, int index) {
        super.add(module);
        if (index > 0) {
            T prev = queue.get(index - 1);
        }
        return module;
    }
    
}
