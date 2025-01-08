/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package codex.renthyl.modules.protocol;

import codex.renthyl.resources.tickets.TicketSignature;

/**
 *
 * @author codex
 */
public interface FilterProtocol extends SignatureProtocol {
    
    /**
     * Gets a signature pointing to the filter's input scene color.
     * 
     * @return 
     */
    public TicketSignature getRenderedSceneColor();
    
    /**
     * Gets a signature pointing to the filter's input scene depth.
     * 
     * @return 
     */
    public TicketSignature getRenderedSceneDepth();
    
    /**
     * Gets a signature pointing to the filter's result.
     * 
     * @return 
     */
    public TicketSignature getFilteredResult();
    
}
