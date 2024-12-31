/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.jobs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author codex
 */
public class JobEventHandler {
    
    private final AtomicBoolean error = new AtomicBoolean(false);
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition complete = lock.newCondition();
    
    public JobEventHandler() {}
    
    private void signalEnd() {
        lock.lock();
        try {
            complete.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Initializes the handler to the current situation.
     * 
     * @param activeJobs 
     */
    public void start(int activeJobs) {
        this.activeJobs.set(activeJobs);
        error.set(false);
    }
    
    /**
     * Signals that an active job experienced a fatal interruption.
     */
    public void interrupt() {
        if (!error.getAndSet(true)) {
            signalEnd();
        }
    }
    
    /**
     * Signals that an active job has effectively completed execution.
     */
    public void complete() {
        if (!error.get() && activeJobs.addAndGet(-1) == 0) {
            signalEnd();
        }
    }
    
    /**
     * Disables the current thread until all jobs have effectively completed.
     * 
     * @param waitMillis
     */
    public void waitForActiveJobs(long waitMillis) {
        try {
            while (isRunning()) {
                if (!complete.await(waitMillis, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Waiting timed out after " + waitMillis + " milliseconds.");
                }
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException("Waiting interrupted.", ex);
        }
    }
    
    /**
     * Returns the number of jobs currently executing.
     * 
     * @return 
     */
    public int getNumActiveJobs() {
        return activeJobs.get();
    }
    
    /**
     * Returns true if an error occured.
     * 
     * @return 
     */
    public boolean errorOccured() {
        return error.get();
    }
    
    /**
     * Returns true if a job is currently running.
     * 
     * @return 
     */
    public boolean isRunning() {
        return !error.get() && activeJobs.get() > 0;
    }
    
}
