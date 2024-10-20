/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

/**
 *
 * @author codex
 * @param <T>
 */
public interface AutoConnector <T extends RenderModule> {
    
    public void connect(RenderContainer<T> container, T module);
    
}
