/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

/**
 *
 * @author codex
 */
public class IntArrayDef extends AbstractArrayDef<int[]> {

    public IntArrayDef() {}
    public IntArrayDef(int size) {
        super(size);
    }
    
    @Override
    public int[] createResource() {
        return new int[size + padding];
    }
    @Override
    public float evaluateResource(Object resource) {
        if (resource instanceof int[]) {
            int[] array = (int[])resource;
            if (array.length >= size) {
                return 0;
            }
        }
        return Float.POSITIVE_INFINITY;
    }
    @Override
    public int[] applyResource(Object resource) {
        return (int[])resource;
    }
    
}
