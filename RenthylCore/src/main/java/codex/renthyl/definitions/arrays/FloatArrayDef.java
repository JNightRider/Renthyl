/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

import codex.renthyl.resources.ResourceException;

/**
 *
 * @author codex
 */
public class FloatArrayDef extends AbstractArrayDef<float[]> {

    public FloatArrayDef() {}
    public FloatArrayDef(int size) {
        super(size);
    }
    
    @Override
    public float[] createResource() {
        return new float[size + padding];
    }
    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof float[]) {
            float[] array = (float[])resource;
            if (array.length >= size) {
                return 0f;
            }
        }
        return null;
    }
    @Override
    public float[] conformResource(Object resource) throws ResourceException {
        return (float[])resource;
    }
    
}
