/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

import java.lang.reflect.Array;

/**
 * Resource definition for object arrays.
 *
 * @author codex
 * @param <T> array component type
 */
public class ArrayDef <T> extends AbstractArrayDef<T[]> {
    
    private Class<T> type;
    
    public ArrayDef() {}
    public ArrayDef(Class<T> type) {
        this(type, 1);
    }
    public ArrayDef(Class<T> type, int size) {
        super(size);
        this.type = type;
    }
    
    @Override
    public T[] createResource() {
        return (T[])Array.newInstance(type, size + padding);
    }
    @Override
    public Float evaluateResource(Object resource) {
        Class component = resource.getClass().getComponentType();
        if (component != null && type.isAssignableFrom(component)) {
            T[] array = (T[])resource;
            if (array.length >= size) {
                return 0f;
            }
        }
        return null;
    }
    @Override
    public T[] conformResource(Object resource) {
        return (T[])resource;
    }

    /**
     * Gets the component type of arrays.
     *
     * @return
     */
    public Class<T> getType() {
        return type;
    }
    
}
