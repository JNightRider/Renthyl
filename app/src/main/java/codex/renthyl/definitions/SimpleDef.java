/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions;

/**
 *
 * @author codex
 */
public class SimpleDef <T> extends AbstractResourceDef<T> {
    
    private final Class<T> type;

    public SimpleDef(Class<T> type) {
        this.type = type;
    }
    
    @Override
    public T createResource() {
        return null;
    }
    @Override
    public T applyDirectResource(Object resource) {
        if (type.isAssignableFrom(resource.getClass())) {
            return (T)resource;
        }
        return null;
    }
    @Override
    public T applyIndirectResource(Object resource) {
        return null;
    }
    @Override
    public boolean isAllowIndirectResources() {
        return false;
    }
    
}
