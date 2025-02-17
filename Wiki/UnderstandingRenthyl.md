# Understanding Renthyl

The key to using Renthyl effectively is to understand how Renthyl behaves and why it does things the way it does. The document will attempt to explain how Renthyl works, and why that matters to developers.

## Core Design Goals

Renthyl was designed with three main goals in mind:

1. **Modular pipeline.** Modular means that the pipeline is divided into individual modules which can easily be removed, added, or totally rewritten and replaced. This architecture choice is powerful for rendering because we don't expect that every game will work well with generic rendering techniques. In other words, modularity gives developers the flexibility to redesign any individual part of the rendering pipeline that they please.

2. **Resource management.** This goal is incredibly important for games because poorly managed resources can easily cost precious milliseconds that could be used elsewhere. We've implemented a resource system that reallocates unused resources where applicable to minimize the overall memory footprint.

3. **Communication between modules.** To emphasize the pipeline's modularity, modules must be able to communicate or share resources. For example, if one module writes to a texture, another module should be able to read the contents of that texture.

Renthyl meets the first goal specifically by providing the RenderModule interface. RenderModules can be queued into the FrameGraph and get executed when the FrameGraph is called for rendering. In this sense, a FrameGraph is nothing more than a task queue, and RenderModules are the tasks to perform.

The other goal is met by the ResourceList class. ResourceList is the public-facing API through which RenderModules can interact with the resource system as a whole. The resource system is quite a bit larger than just ResourceList, but only ResourceList can be publicly interacted with.

The communication medium between these two components are ResourceTickets. Whenever a RenderModule wants to perform an operation on a resource, a ResourceTicket is used to identify the resource in the ResourceList.

Finally, Renthyl meets the third goal by ResourceTickets. Instead of physically passing resources between modules, Renthyl chooses to simply share the contents of ResourceTickets between modules. Two different modules using tickets with the same contents will act on the same resource.

## Components

* **RenderPass.** An extension of RenderModule specifically built to facilitate rendering tasks. Most rendering tasks implement this class instead of extending RenderModule directly.
* **ResourceDef.** ResourceDefs represent another communication medium between RenderModules and the resource system. They control which resources are reallocated and construct new resources when necessary. Whenever a RenderModule wishes to create a resource, a ResourceDef must be provided to manage its creation.
* **TicketGroups.** TicketGroups store and manage ResourceTickets within individual RenderModules. Each RenderModule contains at least two TicketGroups called main groups (one to manage input tickets and another to manage output tickets).
* **FGRenderContext.** Provides RenderModules with direct access to the JME's RenderManager, AppProfiler, Renderer, the current ViewPort, as well as FrameGraph fields and helper methods.
* **FGJobExecutor.** Renthyl provides support for multithreading the rendering pipeline, and FGJobExecutor is responsible for executing jobs that are not designated to run on JME's main thread.

## Terms

* **Connection.** A connection from one ResourceTicket to another, which allows resources to be shared from the source ticket to the target ticket.
* **FrameGraph layout.** The layout of modules within the FrameGraph and the connections between them. The layout changes if a module is added or removed, or if a resource connection between two modules is made or broken. Renthyl tracks layout to determine if some layout-specific operations can be skipped in favor of using computations from last frame.

## Rendering Stages

The FrameGraph rendering pipeline is split into five seperate stages.

1. **Update.** For general-purpose updates that must be done before the preparation stage. This is not commonly used, but it is always invoked for every module, unlike subsequent stages. It is also performed before the FrameGraph's layout is checked for queueing.
2. **Queueing.** Although the FrameGraph is already a queue by design, it may contain modules that need to be executed on other threads. This pass sorts passes into FGExecutionJobs which are later executed on the appropriate threads. This stage can be skipped for all modules if the FrameGraph's layout has not changed.
3. **Preparation.** This stage is primarily for synchronizing the resource system with how modules plan on using resources during execution. A module only performs this stage if the FrameGraph's layout changed or if it was not culled in culling stage last render frame.
4. **Culling.** Based on how resources are being used, the resource system progressively culls modules whose output resources are unnecessary. This stage is skipped if the FrameGraph's layout was not changed.
5. **Execution.** Modules that have not been culled up to this point are executed. That is, modules perform rendering operations.
6. **Reset.** All modules are reset. This stage is often not implemented in many module implementations.

Note that stages 2, 3, and 4 are influenced by temporal culling, which reuses queueing and culling from a previous frame to determine which modules should be invoked, or if a particular stage should be used at all.

# In Practice

## Custom RenderPass

RenderPass is an implementation of RenderModule specifically designed to facilitate rendering tasks. RenderPass provides 5 abstract methods to implement:

```java
public class MyCustomPass extends RenderPass {
    
    @Override
    public void initialize(FrameGraph frameGraph) { }
    
    @Override
    public void prepare(FGRenderContext context) { }
    
    @Override
    public void execute(FGRenderContext context) { }
    
    @Override
    public void reset(FGRenderContext context) { }
    
    @Override
    public void cleanup(FrameGraph frameGraph) { }
    
}
```

Prepare, execute, and reset are called during the corresponding pipeline stages, and are subject to the culling of those particular stages. Initialize and cleanup are not part of the pipeline, but are instead called when the module is attached to or removed from the FrameGraph, respectively.

### Creating ResourceTickets to Use

For each resource the pass intends on using during execution, a corresponding ResourceTicket must be created and stored in the pass. For ResourceTickets corresponding to resources received from other passes, the ticket must be registered to an input TicketGroup, and ResourceTickets for resources produced as output from this pass must be registered to an output TicketGroup. By default, RenderPass is initialized with one input TicketGroup and one output TicketGroup (known as main groups).

This demonstration will use a single input resource and produce a single output resource, therefore two tickets are required. 

```java
private ResourceTicket<Texture2D> myInputTicket;
private ResourceTicket<Texture2D> myOutputTicket;
```

Tickets should only be registered to a TicketGroup after or during initialization (preferably during). RenderPass provides several protected methods for registering and creating tickets. Two of those methods are `addInput` and `addOutput`.

```java
public void initialize(FrameGraph frameGraph) {
    myInputTicket = addInput("MyInput");
    myOutputTicket = addOutput("MyOutput");
}
```

The names assigned to the tickets ("MyInput" and "MyOutput") are important for allowing other RenderModules to locate and connect to the tickets. The `addInput` and `addOutput` methods create the ResourceTickets as part of the main input TicketGroup and main output TicketGroup, respectively.

### Creating a ResourceDef for Output Resources

The one output resource in the demonstration requires a ResourceDef to manage how to is created and/or how other resources can be reallocated to be the output resource. Since the output resource is a `Texture2D`, a general purpose implementation of ResourceDef for textures will be used: TextureDef.

```java
private final TextureDef<Texture2D> myOutputDef = TextureDef.texture2D();
```

ResourceDef implementations often provide methods that control aspects of the corresponding resource. For example, TextureDef provides `setSize` which controls the size of the corresponding texture resource.

```java
myOutputDef.setSize(1024, 1024);
```

### Using ResourceTickets to Plan Resources

Any resources that are used during execution must first be either declared or referenced during the preparation stage. Resources received from other modules must be referenced, and likewise resources produced by this pass must be declared along with a ResourceDef. Be sure not to declare input resources or reference output resources.

For ease of use, the protected field `resources` is the ResourceList for the FrameGraph, however, RenderPass provides many protected methods that help call ResourceList methods. Two of those methods are `reference` and `declare`.

```java
public void prepare(FGRenderContext context) {
    reference(myInputTicket);
    declare(myOutputDef, myOutputTicket);
}
```

### Using Resources during Execution

The resources requested during preparation can be accessed only during execution using acquiring methods.

```java
public void execute(FGRenderContext context) {
    Texture2D myInputResource = resources.acquire(myInputTicket);
    Texture2D myOutputResource = resources.acquire(myOutputTicket);
    // do rendering task...
}
```

Note that `myOutputResource` will have the same properties defined by `myOutputDef` at the time `acquire` is called. Suppose that `myOutputResource` should have the same size as `myInputResource`. This is possible by assigning `myOutputDef` with the size of `myInputResource` before acquiring `myOutputResource`.

```java
public void execute(FGRenderContext context) {
    Texture2D myInputResource = resources.acquire(myInputTicket);
    int w = myInputResource.getImage().getWidth();
    int h = myInputResource.getImage().getHeight();
    myOutputDef.setSize(w, h);
    Texture2D myOutputResource = resources.acquire(myOutputTicket);
    // do rendering task...
}
```

### FrameBuffers

RenderPass and ResourceList together provide a simple but effective framebuffer handling system that can be used during the execution stage. RenderPass manages the framebuffers themselves, and ResourceList handles the texture attachments of the framebuffers.

```java
int bufferWidth = ...
int bufferHeight = ...
int bufferSamples = ...
FrameBuffer fb = getFrameBuffer(bufferWidth, bufferHeight, bufferSamples);
resources.acquireColorTarget(fb, myColorTargetTicket);
resources.acquireDepthTarget(fb, myDepthTargetTicket);
```

* `getFrameBuffer` either retrieves a stored framebuffer with the given width, height, and number of samples, or creates a new framebuffer with those properties.
* `acquireColorTarget` simultaneously acquires the resource corresponding to the ResourceTicket and attaches the resource as a color target of the framebuffer. This method specifically tries to avoid unnecessary and slow texture binds.
* `acquireDepthTarget` is the same as `acquireColorTarget`, but for depth targets.

Note that if a framebuffer stored in a RenderPass is not accessed for an entire frame, the RenderPass will destroy it.

### RenderModes

With a modular system like Renthyl, it is not unlikely that the user may accidentally design their RenderPasses so that rendering settings get leaked between modules. RenderModes solve this problem by both setting the render setting and resetting it after the current module is finished executing.

```java
public void execute(FGRenderContext context) {
    Material forcedMat = ...
    context.registerMode(RenderMode.forcedMaterial(forcedMat));
}
```

### TicketGroups

As RenderPasses grow larger, TicketGroups are a great way to organize ResourceTickets. There are several TicketGroup implementations that each handle ResourceTickets differently.

* **TicketList.** The most common implementation. Stores any number of tickets in the order they were added, and tickets can be added or removed beyond the initial group declaration.
* **TicketArray.** Stores a fixed number of tickets defined at group declaration. Tickets can neither be added nor removed.
* **DynamicTicketList.** Contains an arbitrary number of tickets depending on how many outside tickets attempt to connect with tickets in this group. That is, if a ticket attempts to connect as a source to a ticket in this group, a new arbitrarily-named ticket will be created to complete the connection, rather than using an existing ticket. Tickets cannot be directly added or removed from this group. Tickets are ordered chronologically by connection time.
* **DefinedTicketList.** Similar to TicketList, except this group additionally contains a ResourceDef for each ticket.
* **DefinedTicketArray.** Similar to both TicketArray and DefinedTicketList.

TicketGroups can be added to a RenderModule as either an input or output group. If the TicketGroup is added as an input group, all contained ResourceTickets are considered input tickets, and vise versa.

```java
private TicketList<Texture2D> myListGroup;
private ResourceTicket<Texture2D> myInputTicket;

public void initialize(FrameGraph frameGraph) {
    myListGroup = addInputGroup(new TicketList<>("MyListGroup"));
    myInputTicket = myListGroup.add("MyTicket");
}
```

TicketGroups are Iterable, so they can be directly passed into some methods to perform an operation on all tickets within the group.

```java
public void prepare(FGRenderContext context) {
    reference(myListGroup);
}
```

# Constructing FrameGraphs

---

FrameGraphs are constructed by first defining and attaching the set of RenderModules to execute, and then connecting the appropriate ResourceTickets of the RenderModules together.

```java
FrameGraph fg = new FrameGraph(assetManager);
SceneEnqueuePass enqueue = fg.add(SceneEnqueuePass.withSingleQueue());
GeometryPass geometry = fg.add(new GeometryPass());
OutputPass out = fg.add(new OutputPass());
```

The RenderPasses used here are:

* **SceneEnqueuePass.** Takes all geometries in the scene and adds them to a queue for rendering, which are then produced as output. `SceneEnqueuePass.withSingleQueue()` creates an instance containing a single queue to which all geometries are added.
* **GeometryPass.** Renders all geometry in a single input queue to a color texture and a depth texture, which are produced as output.
* **OutputPass.** Renders an input color texture and/or depth texture to the screen.

Ticket connection makes use of TicketSelectors to select which tickets should be connected together. For example, to connect a ticket name "Result" from QueueMergePass to a ticket named "Geometry" from GeometryPass:

```java
geometry.getMainInputGroup().makeInput(enqueue.getMainOutputGroup(),
        TicketSelector.name("Queue"), TicketSelector.name("Geometry"));
```

Now the resource for "Queue" ticket from SceneEnqueuePass is passed into GeometryPass under the "Geometry" ticket.

Note that multiple connections can be made from one `makeInput` call. If, in the above example, GeometryPass contained two input tickets named "Geometry", both those tickets would get connected to "Queue" from SceneEnqueuePass. So, while flexible, it can result in tickets not being connected as expected. A good rule of thumb is to never give two input or output tickets the same name to keep names as a unique identifier.

```java
out.getMainInputGroup().makeInput(geometry.getMainOutputGroup(),
        TicketSelector.names("Color", "Depth"),
        TicketSelector.names("Color", "Depth"), false);
```

Now "Color" is connected to "Color", and "Depth" to "Depth". The last argument for this method ensures source tickets cannot make connections to more than one source ticket. Without it, both "Color" and "Depth" from OutputPass would be connected to "Color" from GeometryPass.
