/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.jobs;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author codex
 */
public class DefaultJobExecutor implements FGJobExecutor {
    
    private final ExecutorService service;
    
    public DefaultJobExecutor() {
        this(Executors.newCachedThreadPool());
    }
    public DefaultJobExecutor(ExecutorService service) {
        this.service = service;
    }
    
    @Override
    public void submitExecutionJobs(Collection<FGExecutionJob> jobs) {
        for (FGExecutionJob j : jobs) {
            service.execute(j);
        }
    }
    
    public void terminate() {
        service.shutdownNow();
    }
    
}
