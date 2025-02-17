# Modules

### Attribute

Attribute is a RenderPass that acts as a medium between the FrameGraph and game logic. Formally, it takes an input of any type and produces an output of the same type. The input resource is passed to the game logic through a set of registered GraphTargets, and the output resource is produced through a registered GraphSource.

```java
Attribute<Float> attr = frameGraph.add(new Attribute<>());
attr.addTarget(new MyCustomTarget());
attr.setSource(new MyCustomSource());
```

If no GraphSource is provided, the output resource is marked as "undefined."

Attribute also has a corresponding GroupAttribute class that can work with multiple inputs and outputs.

### Junction

Junction is a powerful RenderPass implementation that selects one input among any number of inputs to produce as its only output. This is useful, for example, if a filter effect pass should only be used during a particular game state. By running the pass's output through a Junction, a handy switch is created that can be used to enable or disable the pass.

```java
RenderModule myPass1 = ...
RenderModule myPass2 = ...
RenderModule myReceiver = ...
Junction junct = frameGraph.add(new Junction());

junct.getMainInputGroup().makeInput(myPass1.getMainOutputGroup(),
        TicketSelector.name("MyOutput1"),
        TicketSelector.All);

junct.getMainInputGroup().makeInput(myPass2.getMainOutputGroup(),
        TicketSelector.name("MyOutput2"),
        TicketSelector.All);

myReceiver.getMainInputGroup().makeInput(junct.getMainOutputGroup(),
        TicketSelector.name(Junction.OUTPUT), 
        TicketSelector.name("MyInput"));

// select the input from MyOutput1
junct.setIndexSource(GraphSource.value(0));
// select the input from MyOutput2
junct.setIndexSource(GraphSource.value(1));
```

Depending on the value provided via `setIndexSource`, either the output from myPass1 or myPass2 will be effectively fed into "MyInput" from myReceiver.

Note that the Junction doesn't reference the resources that it has not selected to produce as output, so the modules that are providing them can still be culled.

### CacheRead and CacheWrite

Renthyl provides a basic "caching" system that resources can be added to and be expected to not change from frame to frame. Normally resources would be left within the RenderObjectMap where they could be changed. Additionally, the caching system indexes resources based on a string, so it is much easier to find the same resource again. These characteristics make the caching system perfect for temporally-based renderings.

The CacheRead and CacheWrite modules store and fetch from the caching system, respectively. CacheRead receives one input, which it stores at a pre-defined key in the cache. CacheWrite produces one output, which is read from a pre-defined key in the cache.

```java
GraphSource<String> cacheKey = GraphSource.value("MyCachedValue");
CacheRead<Texture2D> read = frameGraph.add(new CacheRead(Texture2D.class, cacheKey));
CacheWrite write = frameGraph.add(new CacheWrite(cacheKey));
```

CacheRead removes the resource from the cache when it acquires it. Resources left too long in the cache are automatically removed.

### GeometryPass, etc.

GeometryPass simply renders geometries contained in an input GeometryQueue to a color texture and a depth texture, which are both produced as outputs.

```java
GeometryPass geometry = frameGraph.add(new GeometryPass());
```

Similar to GeometryPass are GeometryDepthPass (renders only to a depth texture and not a color texture) and OutputGeometryPass (renders to the screen rather than textures).

### OutputPass

OutputPass renders a color texture and/or a depth texture to the screen framebuffer. Formally, it takes two textures and input, and produces no outputs.

```java
OutputPass out = frameGraph.add(new OutputPass());
```

### RenderContainer

RenderContainer contains a set of "child" modules that get updated, queued, etc. as normal. Sort of like a Node, but for RenderModules.

```java
RenderContainer<GeometryPass> container = frameGraph.add(new RenderContainer<>());
GeometryPass g = container.add(new GeometryPass());
```

Note that RenderContainer requires a generic type that can be used to limit what can be added to it. In the above example, only GeometryPasses can be added to the container. This feature is important because extensions of RenderContainer can be made that expect a certain implementation of RenderModule (such as a [protocol](Features.md#signatureprotocol)).

### SceneEnqueuePass

Flattens the geometries of scenes attached to the current ViewPort into a set of predefined GeometryQueues based on name.

```java
SceneEnqueuePass enqueue = frameGraph.add(new SceneEnqueuePass());
enqueue.add("MyQueue1", new GeometryQueue(new OpaqueComparator()));
enqueue.add("MyQueue2", new GeometryQueue(new NullComparator()));
```

The queue that a geometry is added to depends on either `Spatial#getQueueBucket()` or the userdata string value under "RenderQueue", where the enum name corresponds to the queue for the former, and the string corresponds to the queue in the latter. If the userdata value is defined, it overrides `getQueueBucket()`. In the above example, if a geometry (or an ancestor) had "MyQueue2" stored under "RenderQueue" in userdata, that geometry would be added to "MyQueue2".

If the queue to add to cannot be determined (perhaps no queue exists under the provided name), then the geometry is added to the default queue. The default queue is defined as the first queue added, or can be defined via `setDefaultQueue`.

The order queues are added determines the order the corresponding output tickets appear in the main output group of SceneEnqueuePass. In the above example, `enqueue` would contain two output tickets: "MyQueue1" and "MyQueue2" in that order corresponding to each queue.

SceneEnqueuePass provides helper methods for creating SceneEnqueuePass instances with queues already defined. `withLegacyQueues` creates "Opaque", "Sky", "Transparent", "Gui", and "Translucent" queues, in that order. This is designed to emulate JMonkeyEngine's RenderQueue class, where each queue corresponds exactly to a `RenderQueue#Bucket` enum value.

```java
SceneEnqueuePass legacy = frameGraph.add(SceneEnqueuePass.withLegacyQueues());
```

`withSingleQueue` creates a single queue named "Queue". Since it only contains one queue, all geometries will be added to that queue.

```java
SceneEnqueuePass single = frameGraph.add(SceneEnqueuePass.withSingleQueue());
```

### QueueMergePass

Merges any number of input GeometryQueues into a single GeometryQueue, which is produced as output.

```java
QueueMergePass merge = frameGraph.add(new QueueMergePass());
```

GeometryQueue "merging" is done by adding each GeometryQueue to a parent GeometryQueue. All GeometryQueues remain intact and maintain their render settings and sort orders.

QueueMergePass uses a DynamicTicketList as its main input group, so adding inputs to it is fairly straightforward. For example, the following code connects all output queues from SceneEnqueuePass to QueueMergePass.

```java
SceneEnqueuePass enqueue = ...
merge.getMainInputGroup().makeInput(enqueue.getMainOutputGroup(),
        TicketSelector.All, TicketSelector.All);
```
