# Multithreading

Renthyl is able to perform RenderModule executions in parallel using a job system and by using resources as convenient barriers.

### How Multithreading Works

One stage during FrameGraph rendering is the queueing stage, which places all RenderModules into queues (called "jobs") which are then iterated over for execution. Only the first job (index=0) is executed on JMonkeyEngine's main render thread; all other jobs are executed by an FGJobExecutor.

By default, Renthyl uses an implementation of FGJobExecutor called DefaultJobExecutor, which uses an ExecutorService to execute each job on its own thread. However, Renthyl also allows developers to write and use their own FGJobExecutors to execute the jobs however they like using whatever job manager they please.

Renthyl synchronizes thread execution using resources as barriers. When a job comes to a resource that has not yet been acquired by the declaring module, it blocks until that resource becomes available. By default, resources are assumed to be able to handle concurrent reads, but if not, ResourceDef provides `isReadConcurrent` to control this property.

### RenderThread

Ultimately, the RenderModules themselves are responsible for selecting the job they get added to during the queueing stage, but the vast majority of RenderModule implementation opt to be added to the same job as their parent (or the main job at index=0, if no parent exists). RenderThread is a [RenderContainer](Modules.md#rendercontainer) extension that directs all its children to be added to a particular job by index.

```java
RenderThread thread = frameGraph.add(new RenderThread(GraphSource.value(1)));
```

The constructor argument controls which job the RenderThread is added to, where 0 is the main job that runs on JMonkeyEngine's main render thread. The index must be non-negative and has no upper bound. If an index is used that does not correspond to any existing job internally, a new job is created (so index=2 can be used even if index=1 is never used).

### FGJobExecutor

FGJobExecutor as stated above is responsible for executing a collection of FGExecutionJobs.

```java
public class MyJobExecutor implements FGJobExecutor {

    @Override
    public void submitExecutionJobs(Collection<FGExecutionJob> jobs) {
        // submit jobs for execution
    }
    
}
```

Be sure not to directly execute the jobs in `submitExecutionJobs`, which is called on the main render thread. Otherwise, timeout errors will likely occur.

FGJobExecutors can be registered either directly to a FrameGraph or to the global FrameGraph context. In the latter, the FGJobExecutor will be used by all FrameGraphs that do not have an executor explicitely defined.

```java
frameGraph.setExecutor(myExecutor);
renderManager.getContext(FGPipelineContext.class).setDefaultExecutor(myExecutor);
```
