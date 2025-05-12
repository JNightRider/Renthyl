package codex.renthyl.geometry;

import com.jme3.util.ListSort;

import java.util.Arrays;
import java.util.Comparator;

public interface Sorter <T> {

    void sort(T[] array, int size, Comparator<T> comparator);

    default void sort(T[] array, Comparator<T> comparator) {
        sort(array, array.length, comparator);
    }

}
