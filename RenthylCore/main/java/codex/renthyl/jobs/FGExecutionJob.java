/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.jobs;

import codex.renthyl.FGRenderContext;
import codex.renthyl.modules.RenderModule;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class FGExecutionJob implements Runnable, Iterable<RenderModule> {
    
    private final JobEventHandler events;
    private final FGRenderContext context;
    private final LinkedList<RenderModule> queue = new LinkedList<>();
    private final int index;

    public FGExecutionJob(JobEventHandler events, FGRenderContext context, int index) {
        this.events = events;
        this.context = context;
        this.index = index;
    }
    
    @Override
    public void run() {
        try {
            long jobStart = System.nanoTime();
            System.out.println("Job " + index + " profile:");
            for (RenderModule m : queue) {
                long start = System.nanoTime();
                m.executeModuleRender(context);
                System.out.println("  " + m + ": " + ((double)Math.abs(System.nanoTime() - start) / 1000000.0) + "ms");
            }
            System.out.println("Job " + index + " took " + ((double)Math.abs(System.nanoTime() - jobStart) / 1000000.0) + "ms");
            events.complete();
        } catch (Exception ex) {
            events.interrupt();
            throw new RuntimeException("FrameGraph job " + index + " crashed: " + ex.getMessage(), ex);
        }
    }
    @Override
    public Iterator<RenderModule> iterator() {
        return queue.iterator();
    }
    
    /**
     * Adds the module to this job.
     * 
     * @param module 
     * @return the index the module was added to in the module queue
     */
    public int add(RenderModule module) {
        queue.addLast(module);
        return queue.size() - 1;
    }
    
    /**
     * Clears the module queue.
     */
    public void flush() {
        queue.clear();
    }
    
    /**
     * Returns true if the module queue is empty.
     * 
     * @return 
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Returns the index associated with this job.
     * 
     * @return 
     */
    public int getIndex() {
        return index;
    }
    
}
