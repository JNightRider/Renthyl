/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

import codex.renthyl.definitions.ResourceDef;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class AbstractArrayDef <T> implements ResourceDef<T>, Savable {

    protected int size;
    protected int padding;
    
    public AbstractArrayDef() {
        this(1);
    }
    public AbstractArrayDef(int size) {
        this.size = size;
    }

    @Override
    public void dispose(T array) {}

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(size, "size", 1);
        out.write(padding, "padding", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        size = in.readInt("size", 1);
        padding = in.readInt("padding", 0);
    }
    
    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Array size must be greater than zero.");
        }
        this.size = size;
    }
    public void setPadding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Array padding cannot be less than zero.");
        }
        this.padding = padding;
    }

    public int getSize() {
        return size;
    }
    public int getPadding() {
        return padding;
    }
    
}
