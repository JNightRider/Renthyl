/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.util;

import java.util.Iterator;

/**
 *
 * @author codex
 * @param <T>
 */
public class ArrayIterator <T> implements Iterable<T>, Iterator<T> {
    
    private final T[] array;
    private int index = 0;

    public ArrayIterator(T[] array) {
        this.array = array;
    }
    
    @Override
    public Iterator<T> iterator() {
        index = 0;
        return this;
    }
    @Override
    public boolean hasNext() {
        return index < array.length;
    }
    @Override
    public T next() {
        return array[index++];
    }
    
    public T[] getArray() {
        return array;
    }
    
}
