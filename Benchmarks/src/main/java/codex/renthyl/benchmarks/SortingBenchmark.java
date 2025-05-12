package codex.renthyl.benchmarks;

import com.jme3.util.ListSort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class SortingBenchmark {

    public static void main(String[] args) {

        final int trials = 10000;
        final int arrayLength = 3000;
        final Random random = new Random();

        ListSort<Integer> listSort = new ListSort<>();
        listSort.allocateStack(arrayLength);
        long totalNanos = 0;
        for (int i = 0; i < trials; i++) {
            Integer[] array = generateArray(random, arrayLength);
            long start = System.nanoTime();
            listSort.sort(array, Integer::compare);
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("ListSort", totalNanos, trials);

        totalNanos = 0;
        for (int i = 0; i < trials; i++) {
            Integer[] array = generateArray(random, arrayLength);
            long start = System.nanoTime();
            Arrays.sort(array, Integer::compare);
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("Arrays.sort", totalNanos, trials);

        totalNanos = 0;
        for (int i = 0; i < trials; i++) {
            Integer[] array = generateArray(random, arrayLength);
            long start = System.nanoTime();
            Arrays.stream(array).sorted(Integer::compare).findAny();
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("Arrays.stream.sorted", totalNanos, trials);

        totalNanos = 0;
        for (int i = 0; i < trials; i++) {
            int[] array = generateIntArray(random, arrayLength);
            long start = System.nanoTime();
            Arrays.sort(array);
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("Arrays.sort(int[])", totalNanos, trials);

        totalNanos = 0;
        for (int i = 0; i < trials; i++) {
            ArrayList<Integer> list = new ArrayList<>(Arrays.asList(generateArray(random, arrayLength)));
            long start = System.nanoTime();
            list.sort(Integer::compare);
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("ArrayList.sort", totalNanos, trials);

        totalNanos = 0;
        Integer[] sourceArray = generateArray(random, arrayLength);
        for (int i = 0; i < trials; i++) {
            Integer[] targetArray = new Integer[arrayLength];
            long start = System.nanoTime();
            System.arraycopy(sourceArray, 0, targetArray, 0, arrayLength);
            totalNanos += Math.abs(System.nanoTime() - start);
        }
        printResult("System.arraycopy", totalNanos, trials);

    }

    private static Integer[] generateArray(Random random, int length) {
        Integer[] a = new Integer[length];
        for (int i = 0; i < a.length; i++) {
            a[i] = random.nextInt(1000);
        }
        return a;
    }

    private static int[] generateIntArray(Random random, int length) {
        int[] a = new int[length];
        for (int i = 0; i < a.length; i++) {
            a[i] = random.nextInt(1000);
        }
        return a;
    }

    private static void printResult(String methodName, double nanos, int trials) {
        System.out.println(methodName + ": " + (nanos / trials / 1_000_000) + "ms average per trial.");
    }

}
