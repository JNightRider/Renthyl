# Features

### GraphSource and GraphTarget

These two interfaces facilitate on-the-fly communication between game logic and the FrameGraph. GraphSource supplies a value from game logic to the FrameGraph, and GraphTarget goes the other way. RenderPasses will often allow GraphSources or GraphTargets to be registered to supply or listen for certain values.

Note that GraphSource provides the static method `value` that returns a GraphSource implementation that simply returns a value when requested.

### GraphSetting

This is a commonly used implementation of GraphSource that supplies the requested value from the FrameGraph's internal settings map. The game logic only has to store the value at the same key in the settings map in order to communicate the value.

```java
Attribute<Float> attr = ...
GraphSetting<Float> mySetting = new GraphSetting<>("MySetting");
attr.setSource(mySetting);
...
frameGraph.setSetting("MySetting", 10.5f);
```

### TicketSignature

This class contains the necessary information locate a certain set of tickets from the RenderModule rather than the TicketGroup, as TicketSelectors are limited to. Note that the all tickets to select must all be located in the same TicketGroup.

```java
// a signature that locates an ticket named "Color" in an
// input group named "MyGroup".
TicketSignature myTicketSig = new TicketSignature(
        "MyGroup", true, TicketSelector.name("Color"));
...
ArrayList<ResourceTicket> selected = new ArrayList<>();
myTicketSig.selectFrom(myPass, selected);
for (ResourceTicket t : selected) {
    System.out.println("Ticket selected: " + t.getName());
}
```

### SignatureProtocol

This interface is an extension of the base RenderModule interface. Other "protocol" interfaces are expected to extend SignatureProtocol, and then RenderModule implementations that wish to conform to a protocol simply implement that protocol interface.

```
RenderModule
 L SignatureProtocol
   L MyCustomProtocol
     L MyCustomRenderPass
```
*An example of what such a hierarchy would look like.*

The reason this concept is useful is that RenderContainers can require that only RenderModules implementing a particular protocol are allowed to be added, and then the RenderContainer can use the methods supplied by that protocol.

For example, MyCustomProtocol could define a method that returns a TicketSignature that locates an output ticket intended to produce a color texture.

```java
public interface MyCustomProtocol extends SignatureProtocol {
    
    public TicketSignature getRenderedColor();
    
}
```

Then a RenderContainer could use `getRenderedColor` to locate that particular ticket and do something with it. The neat part is that protocols allow the implementing classes great flexibility over where the intended ticket is stored and what it is named.

### GeometryQueue

Geometries processed within a FrameGraph are usually stored in a GeometryQueue. GeometryQueues provide several important services for rendering geometries:

* Sorting according to the rendering camera view.
* Special rendering instructions to be used whenever rendering the contained geometries.
* Queue "merging" (storing of child queues within one parent GeometryQueue).

Most FrameGraphs will usually begin by adding all geometries in the ViewPort's scenes into a set of GeometryQueues.

```java
GeometryQueue queue = new GeometryQueue(new OpaqueComparator());
queue.add(myGeometry);
queue.render(context, GeometryRenderHandler.DEFAULT);
```

### GeometryRenderHandler

This interface is responsible for culling and rendering individual geometries.

```java
public class MyRenderHandler implements GeometryRenderHandler {
    
    @Override
    public void renderGeometry(FGRenderContext context, Geometry geometry) {
        // render the geometry
    }
    
}
```

Methods not shown are `evaluateSpatialVisibility` and `evaluateVolumeVisibility`, which may be overriden to customize when Spatials and BoundingVolumes are culled rendering.

