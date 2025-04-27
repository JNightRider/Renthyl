/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions;

import codex.renthyl.resources.ResourceException;

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
    public Float evaluateResource(Object resource) {
        return type.isAssignableFrom(resource.getClass()) ? 0f : null;
    }
    @Override
    public T conformResource(Object resource) throws ResourceException {
        return (T)resource;
    }
    
}
