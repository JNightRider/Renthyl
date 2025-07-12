package codex.renthyljme.utils;

import java.util.Comparator;

/**
 * Sorts an Object array according to a {@link Comparator}.
 *
 * @param <T>
 */
public interface Sorter <T> {

    /**
     * Sorts the array up to {@code size} using {@code comparator} to determine
     * the correct sort order.
     *
     * @param array array to sort
     * @param size number of elements to sort, beginning at index zero
     * @param comparator compare array elements
     */
    void sort(T[] array, int size, Comparator<T> comparator);

    /**
     * Sorts the entire array using {@code comparator} to determine the correct
     * sort order.
     *
     * @param array
     * @param comparator
     */
    default void sort(T[] array, Comparator<T> comparator) {
        sort(array, array.length, comparator);
    }

}
