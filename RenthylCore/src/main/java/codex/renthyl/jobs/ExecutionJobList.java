/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.jobs;

import codex.renthyl.FGRenderContext;
import codex.renthyl.modules.ModuleIndex;
import codex.renthyl.modules.AbstractRenderModule;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Contains an array of queues, one queue per thread, which
 * modules are added to for execution.
 * 
 * @author codex
 */
public class ExecutionJobList {
    
    private final JobEventHandler events;
    private final FGRenderContext context;
    private final FGExecutionJob mainJob;
    private final ArrayList<FGExecutionJob> jobs = new ArrayList<>();
    private final ModuleIndex tempIndex = new ModuleIndex();
    private int activeJobs = 1;
    
    public ExecutionJobList(JobEventHandler events, FGRenderContext context) {
        this.events = events;
        this.context = context;
        this.mainJob = new FGExecutionJob(this.events, this.context, 0);
    }
    
    /**
     * Adds the module to the queue at the index.
     * <p>
     * If the index is greater than the number of queues,
     * queues will be added.
     * 
     * @param module
     * @param index
     * @return assigned queue index (do not use resulting object)
     */
    public ModuleIndex add(AbstractRenderModule module, int index) {
        FGExecutionJob job;
        if (index == 0) {
            job = mainJob;
        } else {
            while (index > jobs.size()) {
                jobs.add(null);
            }
            job = jobs.get(index-1);
            if (job == null) {
                job = new FGExecutionJob(events, context, index);
                jobs.set(index-1, job);
                activeJobs++;
            }
        }
        tempIndex.set(job.getIndex(), job.add(module));
        return tempIndex;
    }
    
    /**
     * Clears all jobs.
     */
    public void flush() {
        mainJob.flush();
        for (int i = jobs.size()-1; i >= 0; i--) {
            FGExecutionJob job = jobs.get(i);
            if (job != null) {
                if (job.isEmpty()) {
                    jobs.set(i, null);
                    activeJobs--;
                } else {
                    job.flush();
                }
            }
        }
    }
    
    /**
     * Gets the main execution job intended to be run on the main render thread.
     * 
     * @return 
     */
    public FGExecutionJob getMainJob() {
        return mainJob;
    }
    
    /**
     * Gets the asynchronous job at the index
     * 
     * @param i
     * @return job at the index (may be null)
     */
    public FGExecutionJob getAsyncJob(int i) {
        return jobs.get(i);
    }
    
    /**
     * Gathers all active asynchronous jobs and stores them in the target
     * collection.
     * 
     * @param store collection to store jobs, or null to create use a new list
     * @return collection containing all active asynchronous jobs
     */
    public Collection<FGExecutionJob> gatherActiveAsyncJobs(Collection<FGExecutionJob> store) {
        if (store == null) {
            store = new LinkedList<>();
        }
        for (FGExecutionJob j : jobs) if (j != null) {
            store.add(j);
        }
        return store;
    }
    
    /**
     * Gets the number of queues.
     * <p>
     * Null queues are counted.
     * 
     * @return 
     */
    public int size() {
        return jobs.size();
    }
    
    /**
     * Gets the number of queues that are in use.
     * 
     * @return 
     */
    public int getNumActiveJobs() {
        return activeJobs;
    }
    
}
