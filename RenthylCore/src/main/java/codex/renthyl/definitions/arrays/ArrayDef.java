/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

import codex.boost.export.SavableObject;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.lang.reflect.Array;

/**
 *
 * @author codex
 * @param <T>
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
    public T[] applyDirectResource(Object resource) {
        Class component = resource.getClass().componentType();
        if (component != null && type.isAssignableFrom(component)) {
            T[] array = (T[])resource;
            if (array.length >= size) {
                return array;
            }
        }
        return null;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        SavableObject.writeClass(out, type, "componentType", Float.class);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        type = SavableObject.readClass(in, "componentType", Float.class);
    }

    public Class<T> getType() {
        return type;
    }
    
}
