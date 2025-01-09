/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package codex.renthyl.modules;

/**
 *
 * @author codex
 * @param <T>
 */
public interface LoopBuilder <T extends AbstractRenderModule> {
    
    public void initLoop(RenderLoop<T> loop);
    
    public T createIteration(RenderLoop<T> loop);
    
    public default void removeIteration(RenderLoop<T> loop, T iteration, int i) {
        loop.remove(iteration);
    }
    
}
