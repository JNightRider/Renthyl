# Renthyl Guide

Renthyl is a graph-based rendering system, much like other graph or node based systems. Each node performs an operation on a set of inputs, and produces a set of outputs, which can then be used as input by other nodes. Nodes in Renthyl are represented by the Renderable interface (also called tasks or passes). The inputs and outputs are both covered by the Socket interface. Thus, a Renthyl rendering graph (FrameGraph) consists of interconnected Renderables by their Sockets.

Sockets perform three main functions:

1. Identify the tasks which the current task depends on. This is used for determining what tasks should be rendered and in what order.
2. Block the current task from rendering until all dependency tasks have completed. In other words, it acts as a natural asynchronous barrier.
3. Transfer the resource from the dependency task to the current task. The resource is, after all, why the current task depends on the dependency task in the first place.

Renderables, on the other hand, are where rendering is performed. They generally not directly involved with graph structure management, and instead delegate such management to their sockets.

## Pipeline

FrameGraphs are rendered over 5 steps in order:

1. Staging
2. Update
3. Prepare
4. Render
5. Reset

### Staging

Staging is arguably the most complicated and chaotic of the bunch. During this step, tasks are staged into a RenderingQueue to eventually be rendered. This is important since Renthyl does not require a task to be explicitely registered with a FrameGraph in order to be rendered by that FrameGraph. Instead it only must be a dependency of a task that is being staged.

Staging is performed by first calling the *stage* method on for each task directly registered with the FrameGraph being rendered. Those tasks immediately (before actually staging themselves into the RenderingQueue) call the stage methods of their dependencies through their sockets. This will accurately flatten the graph into a queue, where the least dependent tasks run first, and the most dependent tasks run last. This also ensures that tasks that aren't really necessary don't get staged, and thus don't get rendered.

The drawback (which task implementations must be prepared for) is that a task's *stage* method may be called more than once during the staging step.

### Update

This is exactly what is sounds like. All staged tasks are updated.

### Prepare

This step requires that tasks *reference* the resources they plan on using during the render step. Socket make this really easy by referencing their upstream socket when referenced themselves. All the task has to do is reference it's own sockets.

### Render

This step is where the actual rendering and other related operations occur. After performing render, each task must release all the resources it referenced during the prepare step. Otherwise, at best an exception may occur, or at worst the next render frame will lock up for no apparent reason.

### Reset

Again, this is exactly what it sounds like. All staged tasks are reset.
