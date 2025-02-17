# Porting Filters from JME to Renthyl

For porting JMonkeyengine `Filters` to the Renthyl API, RenthylPlus provides the JmeFilterPass class, which is an extension of RenderPass. The feel of JmeFilterPass is designed to be as close as possible to Filter (with some improvements), so users familiar with Filter will likewise be familiar with JmeFilterPass.

```java
public class PortedFogFilter extends JmeFilterPass {
    
    @Override
    protected void init(FrameGraph frameGraph) {
        
    }

}
```

The immediately noticeable difference from Filter is that no `getMaterial` method is required. JmeFilterPass works totally from `Subpasses` (similar to `Filter.Pass`), so it has no need to recognize a particular material as the "main" one. This example is porting [FogFilter](https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-effects/src/main/java/com/jme3/post/filters/FogFilter.java), so only one Subpass is necessary to initialize.

```java
private ColorRGBA fogColor;
private float fogDensity, fogDistance;

@Override
protected void init(FrameGraph frameGraph) {
    Material mat = new Material(frameGraph.getAssetManager(), 
            "Common/MatDefs/Post/Fog.j3md");
    Subpass myPass = add(new Subpass(mat, true, true) {
        @Override
        public void beforeRender(FGRenderContext context) {
            getMaterial().setColor("FogColor", fogColor);
            getMaterial().setFloat("FogDensity", fogDensity);
            getMaterial().setFloat("FogDistance", fogDistance);
        }
    });
}
```

* The Subpass uses the provided material to render the input color and depth textures to an internal result color texture.
* The two boolean arguments to Subpass indicate that the Subpass requires both the input color texture and input depth texture, respectively, in order to function; Filter would require overriding `isRequiresSceneAsTexture` and `isRequiresDepthAsTexture` to do the same thing.
* The protected `add` method simply adds the Subpass to an internal queue. Subpasses are rendered in the order they were added.
* `beforeRender` is called before the Subpass is rendered, of course. Subpass also provides `beforeAcquire` (called before resources are acquired for this Subpass) and `afterRender` that are meant to be overriden.

### Chaining Subpasses

Subpasses can be "chained" together like Filter.Pass to produce more complicated effects. It works simply by passing the result texture from one Subpass to the another's material before render.

```java
Material mat1 = ...
Subpass pass1 = add(new Subpass(mat1, true, true));

Material mat2 = ...
Subpass pass2 = add(new Subpass(mat2, true, true) {
    @Override
    public void beforeRender(FGRenderContext context) {
        getMaterial().setTexture("PrevPassTexture", pass1.getRenderedTexture());
    }
});
```

### Scene Renders

Some Filter implementations, such as BloomPass and CartoonEdgePass, render the scene again in order to produce the desired effect. Renthyl does not provide direct emulation of this functionality since it is already baked into what Renthyl is. Simply have another pass perform the rendering and pass the resulting texture(s) into the JmeFilterPass. Then other modules can use that texture instead of it being private to the JmeFilterPass.

```java
private ResourceTicket<Texture2D> normals;

@Override
protected void init(FrameGraph frameGraph) {
    normals = addInput("Normals");
    Material mat = ...
    Subpass myPass = add(new Subpass(mat, true, true) {
        @Override
        public void beforeRender(FGRenderContext context) {
            getMaterial().setTexture("Normals", resources.acquire(normals));
        }
    });
}
```

## FilterChain

As a replacement of JMonkeyEngine's FilterPostProcessor, Renthyl provides FilterChain. FilterChain is an extension of RenderContainer that only accepts implementors of FilterProtocol (such as JmeFilterPass).

```java
FilterChain chain = frameGraph.add(new FilterChain());
chain.add(new PortedFogFilter());
chain.add(new SoftBloomFilter());
```

Because FilterChain only accepts FilterProtocol modules, it is able to automatically generate the necessary connections internally when a module is added to it. Additionally, FilterChain itself also implements FilterProtocol, so FilterChains can be nested.
