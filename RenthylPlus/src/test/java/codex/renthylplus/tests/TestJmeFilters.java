/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.renthyljme.FrameGraphContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.render.RenderEnvironment;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.tasks.filter.FilterChain;
import codex.renthyljme.tasks.filter.PostProcessFilter;
import codex.renthyljme.tasks.scene.ControlRenderPass;
import codex.renthyljme.tasks.scene.GeometryPass;
import codex.renthyljme.tasks.scene.OutputPass;
import codex.renthyljme.tasks.scene.SceneEnqueuePass;
import codex.renthyl.tasks.utils.Multiplexor;
import codex.renthylplus.effects.ports.*;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.Shader;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author codex
 */
public class TestJmeFilters extends SimpleApplication {
    
    private GeometryPass geometry;
    private FilterCycle cycle;
    private Multiplexor<Texture2D> channels;
    private BitmapText filterLabel, formatLabel;
    private int colorFormat = 0;
    
    public static void main(String[] args) {
        TestJmeFilters app = new TestJmeFilters();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(700);
        settings.setHeight(700);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {

        assetManager.registerLocator("", ImmediateShader.class);
        flyCam.setMoveSpeed(10);
        flyCam.setDragToRotate(true);
        
        setupFrameGraph();
        setupScene();
        setupGui();
        setupInput();
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }
    
    private void setupFrameGraph() {

        assetManager.registerLocator("", ImmediateShader.class);

        ResourceAllocationState allocator = new ResourceAllocationState();
        allocator.capture(cap -> cap.print(System.err, 20));
        stateManager.attach(allocator);

        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);
        
        fg.addTask(new ControlRenderPass());
        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        geometry = new GeometryPass(allocator);
        NormalPass normals = new NormalPass(assetManager, allocator);
        cycle = new FilterCycle();
        channels = new Multiplexor<>();
        OutputPass out = fg.addTask(new OutputPass());

        // test chaining filters together
        FilterChain chain = cycle.add(new FilterChain());
        CartoonEdgePass cartoon2 = chain.add(new CartoonEdgePass(assetManager, allocator));
        cartoon2.getEdgeColor().setValue(ColorRGBA.Green);
        cartoon2.getNormals().setUpstream(normals.getOutColor());
        chain.add(new FXAAPass(assetManager, allocator));
        chain.add(new ContrastAdjustmentPass(assetManager, allocator));
        chain.add(new DepthOfFieldPass(assetManager, allocator));
        chain.add(new FogPass(assetManager, allocator));

        // screenspace reflections
        SSRPass ssr = cycle.add(new SSRPass(assetManager, allocator));
        ssr.getNormals().setUpstream(normals.getOutColor());
        ssr.getBlurScale().setValue(0.1f);
        ssr.getApproximateNormals().setValue(false);
        ssr.getNumBlurPasses().setValue(0);
        ssr.getReflectionFactor().setValue(100f);

        // soft bloom
        SoftBloomPass softBloom = cycle.add(new SoftBloomPass(assetManager, allocator));
        softBloom.getFactor().setValue(0.3f);
        softBloom.getNumSamplingSteps().setValue(5);

        // cartoon edge
        CartoonEdgePass cartoon = cycle.add(new CartoonEdgePass(assetManager, allocator));
        cartoon.getEdgeColor().setValue(ColorRGBA.Black);
        cartoon.getNormals().setUpstream(normals.getOutColor());

        // screenspace ambient occlusion
        SSAOPass ssao = cycle.add(new SSAOPass(assetManager, allocator, 5, 10, 0.2f, 0.1f));
        ssao.getNormals().setUpstream(normals.getOutColor());
        ssao.getRadius().setValue(0.5f);

        // crosshatch
        CrossHatchPass crosshatch = cycle.add(new CrossHatchPass(assetManager, allocator));

        // anti-aliasing
        FXAAPass fxaa = cycle.add(new FXAAPass(assetManager, allocator));

        // posterization
        PosterizationPass poster = cycle.add(new PosterizationPass(assetManager, allocator));

        // contrast
        ContrastAdjustmentPass contrast = cycle.add(new ContrastAdjustmentPass(assetManager, allocator, 2f));

        // guassian blur bloom
        BloomPass bloom = cycle.add(new BloomPass(assetManager, allocator));

        // depth of field
        DepthOfFieldPass depth = cycle.add(new DepthOfFieldPass(assetManager, allocator));

        // fog
        FogPass fog = cycle.add(new FogPass(assetManager, allocator, ColorRGBA.Black, 5, 100));

        // tonemapping
        FilmicToneMapPass toneMap = cycle.add(new FilmicToneMapPass(assetManager, allocator, new Vector3f(0.5f, 0.5f, 0.5f)));

        geometry.getGeometry().addMapSource(enqueue.getQueues());
        normals.getGeometry().addMapSource(enqueue.getQueues());
        cycle.getSceneColor().setUpstream(geometry.getOutColor());
        cycle.getSceneDepth().setUpstream(geometry.getOutDepth());
        channels.addUpstream(cycle.getFilterResult());
        channels.addUpstream(geometry.getOutColor());
        channels.addUpstream(geometry.getOutDepth());
        channels.addUpstream(normals.getOutColor());
        out.getColor().setUpstream(channels);

        channels.getIndex().setValue(0);
        
    }

    private void setupScene() {
        
        createCube(0, 0, 0, 1, 1, 1, ColorRGBA.Blue);
        createCube(0, 0, 0, 2, 0.3f, 0.3f, ColorRGBA.Red);
        
        PointLight pl = new PointLight();
        pl.setColor(ColorRGBA.White.mult(2f));
        pl.setPosition(new Vector3f(2, 4, 3));
        pl.setRadius(20);
        rootNode.addLight(pl);
        
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.1f)));
        
    }
    private void setupGui() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        filterLabel = new BitmapText(guiFont);
        filterLabel.setSize(guiFont.getCharSet().getRenderedSize());
        filterLabel.setText("");
        filterLabel.setLocalTranslation((float)context.getFramebufferWidth()/2 + 5, context.getFramebufferHeight() - 5, 0);
        guiNode.attachChild(filterLabel);
        formatLabel = new BitmapText(guiFont);
        formatLabel.setSize(guiFont.getCharSet().getRenderedSize());
        formatLabel.setText("Format: RGBA8");
        formatLabel.setLocalTranslation(5, context.getFramebufferHeight() - 5, 0);
        guiNode.attachChild(formatLabel);
        updateGui(cycle.getActiveFilter().getClass().getSimpleName());
    }
    private void setupInput() {
        inputManager.addMapping("nextFilter", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("nextColorOut", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("nextFormat", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addListener((ActionListener) (String name, boolean isPressed, float tpf) -> {
            if (isPressed) {
                switch (name) {
                    case "nextFilter":
                        cycle.increment();
                        updateGui(cycle.getActiveFilter().getClass().getSimpleName());
                        break;
                    case "nextColorOut":
                        int i = channels.getIndex().preview() + 1;
                        switch (i) {
                            case 4: i = 0;
                            case 0: updateGui(cycle.getActiveFilter().getClass().getSimpleName()); break;
                            case 1: updateGui("Scene"); break;
                            case 2: updateGui("Depth"); break;
                            case 3: updateGui("Normals"); break;
                        }
                        channels.getIndex().setValue(i);
                        break;
                    case "nextFormat":
                        switch (++colorFormat) {
                            case 0:
                                geometry.getColorDef().setFormat(Image.Format.RGBA8);
                                formatLabel.setText("Format: RGBA8");
                                break;
                            case 1:
                                geometry.getColorDef().setFormat(Image.Format.RGBA16F);
                                formatLabel.setText("Format: RGBA16F");
                                break;
                            case 2:
                                geometry.getColorDef().setFormat(Image.Format.RGBA32F);
                                formatLabel.setText("Format: RGBA32F");
                                colorFormat = -1;
                                break;
                        }
                        break;
                }
            }
        }, "nextFilter", "nextColorOut", "nextFormat");
    }
    
    private void updateGui(String text) {
        filterLabel.setText(text);
    }
    private Geometry createCube(float x, float y, float z, float w, float h, float d, ColorRGBA color) {
        Geometry g = new Geometry("box", new Box(w, h, d));
        g.setLocalTranslation(x, y, z);
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", color);
        m.setColor("GlowColor", color);
        g.setMaterial(m);
        rootNode.attachChild(g);
        return g;
    }
    
    private static class NormalPass extends GeometryPass implements RenderEnvironment {

        private static ImmediateMatDef matdef;
        private final Material material;

        public NormalPass(AssetManager assetManager, ResourceAllocator allocator) {
            super(allocator);
            setEnvironment(this);
            if (matdef == null) {
                ImmediateShader vert = new ImmediateShader(Shader.ShaderType.Vertex, true)
                        .includeGlslCompat().includeInstancing().includeSkinning().includeMorphing()
                        .attribute("vec3", "inPosition").attribute("vec3", "inNormal")
                        .varying("vec3", "wNormal")
                        .main()
                        .assign("vec4", "modelSpacePos", "vec4(inPosition, 1.0)")
                        .assign("vec3", "modelSpaceNorm", "inNormal")
                        .ifdef("NUM_MORPH_TARGETS")
                        .call("Morph_Compute", "modelSpacePos", "modelSpaceNorm")
                        .endif()
                        .ifdef("NUM_BONES")
                        .call("Skinning_Compute", "modelSpacePos", "modelSpaceNorm")
                        .endif()
                        .assign("gl_Position", "TransformWorldViewProjection(modelSpacePos)")
                        .assign("wNormal", "TransformWorldNormal(modelSpaceNorm)")
                        .end();
                ImmediateShader frag = new ImmediateShader(Shader.ShaderType.Fragment, true)
                        .includeGlslCompat()
                        .varying("vec3", "wNormal")
                        .main()
                        .assign("gl_FragColor.rgb", "wNormal")
                        .end();
                matdef = new ImmediateMatDef(assetManager, "Normals");
                matdef.createTechnique("PreNormalPass")
                        .setVersions(450, 310, 150)
                        .setShader(vert).setShader(frag)
                        .addWorldParameters("WorldViewProjectionMatrix", "WorldNormalMatrix")
                        .add();
            }
            material = matdef.createMaterial();
        }

        @Override
        public void applySettings(FrameGraphContext context) {
            context.getForcedTechnique().pushValue("PreNormalPass");
            context.getForcedMaterial().pushValue(material);
        }

        @Override
        public void restoreSettings(FrameGraphContext context) {
            context.getForcedTechnique().pop();
            context.getForcedMaterial().pop();
        }

    }

    private static class FilterCycle extends Frame implements PostProcessFilter {

        public final TransitiveSocket<Texture2D> color = new TransitiveSocket<>(this);
        public final TransitiveSocket<Texture2D> depth = new TransitiveSocket<>(this);
        public final Multiplexor<Texture2D> result = new Multiplexor<>();
        public final List<PostProcessFilter> filters = new ArrayList<>();

        public FilterCycle() {
            addSockets(color, depth, result);
            result.getIndex().setValue(0);
        }

        public <T extends PostProcessFilter> T add(T filter) {
            filter.getSceneColor().setUpstream(color);
            filter.getSceneDepth().setUpstream(depth);
            result.addUpstream(filter.getFilterResult());
            filters.add(filter);
            return filter;
        }

        @Override
        public PointerSocket<Texture2D> getSceneColor() {
            return color;
        }

        @Override
        public PointerSocket<Texture2D> getSceneDepth() {
            return depth;
        }

        @Override
        public Socket<Texture2D> getFilterResult() {
            return result;
        }

        public PostProcessFilter getActiveFilter() {
            return filters.get(result.getIndex().preview());
        }

        public void increment() {
            int i = result.getIndex().preview();
            result.getIndex().setValue(++i < filters.size() ? i : 0);
        }

    }
    
}
