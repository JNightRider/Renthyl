/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.definitions.arrays;

import codex.renthyl.definitions.ResourceDef;
import java.io.IOException;

/**
 * Resource definition for arrays.
 *
 * @author codex
 * @param <T> array type (i.e. {@code Integer[]})
 */
public abstract class AbstractArrayDef <T> implements ResourceDef<T> {

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

    /**
     * Sets the minimum size for arrays.
     *
     * @param size
     */
    public void setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Array size must be greater than zero.");
        }
        this.size = size;
    }

    /**
     * Sets the length above {@link #getSize() size} at which arrays should be created.
     *
     * @param padding
     */
    public void setPadding(int padding) {
        if (padding < 0) {
            throw new IllegalArgumentException("Array padding cannot be less than zero.");
        }
        this.padding = padding;
    }

    /**
     * Gets the minimum size for arrays.
     *
     * @return
     */
    public int getSize() {
        return size;
    }

    /**
     * Gets the length above {@link #getSize() size} at which arrays should be created.
     *
     * @return
     */
    public int getPadding() {
        return padding;
    }
    
}
