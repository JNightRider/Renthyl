/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package codex.renthyl.modules.protocol;

import codex.renthyl.resources.tickets.TicketSelector;

/**
 *
 * @author codex
 */
public interface FilterProtocol extends ModuleProtocol {
    
    public TicketSelector getInputColor();
    public TicketSelector getInputDepth();
    public TicketSelector getResult();
    
}
