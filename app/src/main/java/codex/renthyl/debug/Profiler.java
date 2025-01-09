/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.debug;

/**
 *
 * @author codex
 */
public class Profiler {
    
    private static long start;
    
    public static void start() {
        start = System.nanoTime();
    }
    
    public static void reset(String task) {
        System.out.println(task + ": " + ((double)Math.abs(System.nanoTime() - start) / 1000000.0) + "ms");
        start();
    }
    
}
