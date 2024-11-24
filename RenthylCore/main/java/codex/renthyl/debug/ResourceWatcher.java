/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.debug;

import codex.renthyl.resources.ResourceView;

/**
 *
 * @author codex
 */
public interface ResourceWatcher {
    
    public void release(ResourceView resource);
    
    public static ResourceWatcher toConsole() {
        return new ConsolePrintWatcher();
    }
    
    public static class ConsolePrintWatcher implements ResourceWatcher {

        @Override
        public void release(ResourceView resource) {
            System.out.println(resource + " released. Now has " + resource.getNumReferences() + " references remaining.");
        }
        
    }
    
}
