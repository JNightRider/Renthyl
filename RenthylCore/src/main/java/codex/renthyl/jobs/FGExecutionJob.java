/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.jobs;

import codex.renthyl.FGRenderContext;
import codex.renthyl.modules.AbstractRenderModule;
import com.jme3.profile.SpStep;
import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class FGExecutionJob implements Runnable, Iterable<AbstractRenderModule> {
    
    private final JobEventHandler events;
    private final FGRenderContext context;
    private final LinkedList<AbstractRenderModule> queue = new LinkedList<>();
    private final int index;

    public FGExecutionJob(JobEventHandler events, FGRenderContext context, int index) {
        this.events = events;
        this.context = context;
        this.index = index;
    }
    
    @Override
    public void run() {
        try {
            for (AbstractRenderModule m : queue) {
                if (m.isUsed()) {
                    if (index == 0 && context.isProfilerAvailable()) {
                        context.getProfiler().spStep(SpStep.ProcPostQueue, m.getName());
                    }
                    m.executeRender(context);
                }
            }
            events.complete();
        } catch (Exception ex) {
            events.interrupt();
            throw new RuntimeException("FrameGraph job " + index + " crashed: " + ex.getMessage(), ex);
        }
    }
    @Override
    public Iterator<AbstractRenderModule> iterator() {
        return queue.iterator();
    }
    
    /**
     * Adds the module to this job.
     * 
     * @param module 
     * @return the index the module was added to in the module queue
     */
    public int add(AbstractRenderModule module) {
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

    /**
     * Returns true if this job is designated to run on the main application thread.
     *
     * @return
     */
    public boolean isMainJob() {
        return index == 0;
    }
    
}
