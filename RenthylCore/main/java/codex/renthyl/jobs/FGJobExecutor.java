/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.jobs;

import java.util.Collection;

/**
 *
 * @author codex
 */
public interface FGJobExecutor {
    
    /**
     * Submits the jobs for execution.
     * <p>
     * Implementations are expected to run each job on different
     * threads in parallel and <em>not</em> on the calling thread.
     * 
     * @param jobs 
     */
    public void submitExecutionJobs(Collection<FGExecutionJob> jobs);
    
}
