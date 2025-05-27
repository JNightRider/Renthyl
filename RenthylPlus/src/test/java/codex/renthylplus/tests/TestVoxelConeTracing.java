/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthylplus.tests;

import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLRenderUtils;
import codex.renthyl.FrameGraph;
import codex.renthyl.resources.ResourceAllocationState;
import codex.renthyl.tasks.scene.ControlRenderPass;
import codex.renthyl.tasks.scene.OutputPass;
import codex.renthyl.tasks.scene.GeometryDepthPass;
import codex.renthyl.tasks.scene.SceneEnqueuePass;
import codex.renthylplus.shadow.ShadowComposerPass;
import codex.renthylplus.shadow.ShadowManager;
import codex.renthylplus.vxgi.LightBufferPass;
import codex.renthylplus.vxgi.LightGatherPass;
import codex.renthylplus.vxgi.VoxelConeTracer;
import com.github.stephengold.wrench.LwjglAssetLoader;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

/**
 *
 * @author codex
 */
public class TestVoxelConeTracing extends SimpleApplication implements AnalogListener {
    
    private int frame = 0;
    private Spatial meshLight;

    public static void main(String[] args) {
        TestVoxelConeTracing app = new TestVoxelConeTracing();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }
    
    @Override
    public void simpleInitApp() {

        GLRenderUtils.initialize(this);
        UniversalShaderLoader.register(assetManager);
        
        assetManager.registerLoader(LwjglAssetLoader.class,
                "3ds", "3mf", "blend", "bvh", "dae", "fbx", "glb", "gltf",
                "lwo", "meshxml", "mesh.xml", "obj", "ply", "stl");
        
        Spatial scene = assetManager.loadModel("Models/gi-test.gltf");
        rootNode.attachChild(scene);
        SceneGraphIterator it = new SceneGraphIterator(scene);
        for (Spatial s : it) {
            for (int i = 0; i < it.getDepth(); i++) {
                System.out.print("  ");
            }
            System.out.println(s.getName());
            if (s.getName().equals("LightCubeMesh")) {
                meshLight = s;
                break;
            }
        }
        if (meshLight == null) {
            throw new NullPointerException("Could not locate mesh light.");
        }

        SpotLight spot = new SpotLight();
        spot.setPosition(new Vector3f(100, 10, 10));
        spot.setDirection(new Vector3f(-1f, -1f, -0.7f).normalizeLocal());
        spot.setColor(ColorRGBA.White.mult(2.0f));
        spot.setSpotRange(1000f);
        spot.setSpotOuterAngle(FastMath.PI*0.25f);
        spot.setSpotInnerAngle(FastMath.PI*0.05f);
        rootNode.addLight(spot);
        
        stateManager.attach(new DetailedProfilerState());
        
        cam.setLocation(new Vector3f(-25, 25, -25));
        cam.setFov(100);
        flyCam.setMoveSpeed(30);
        flyCam.setDragToRotate(true);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);

        fg.addTask(new ControlRenderPass()).setContext(fg.getContext());
        SceneEnqueuePass enqueuePass = SceneEnqueuePass.withLegacyQueues();
        GeometryDepthPass geometryDepth = new GeometryDepthPass(allocator);
        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        ShadowComposerPass composer = new ShadowComposerPass(assetManager, allocator);
        LightGatherPass lightGatherPass = new LightGatherPass();
        LightBufferPass lightBufferPass = new LightBufferPass(allocator);
        VoxelConeTracer vct = new VoxelConeTracer(assetManager, allocator);
        OutputPass out = fg.addTask(new OutputPass());

        geometryDepth.setContext(fg.getContext());
        composer.setContext(fg.getContext());
        lightGatherPass.setContext(fg.getContext());
        out.setContext(fg.getContext());

        geometryDepth.getGeometry().addMapSource(enqueuePass.getQueues());
        shadows.getGeometry().addMapSource(enqueuePass.getQueues());
        composer.getSceneDepth().setUpstream(geometryDepth.getDepth());
        composer.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        lightBufferPass.getLights().addCollectionSource(lightGatherPass.getLights());
        vct.getGeometry().addMapSource(enqueuePass.getQueues());
        vct.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        vct.getLightBuffer().setUpstream(lightBufferPass.getLightBuffer());
        vct.getLightContribution().setUpstream(composer.getLightContribution());
        out.getColor().setUpstream(vct.getResult());
        
        inputManager.addMapping("x+", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("x-", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addMapping("y+", new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("y-", new KeyTrigger(KeyInput.KEY_RCONTROL));
        inputManager.addMapping("z+", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("z-", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(this, "x+,x-,y+,y-,z+,z-".split(","));
        
    }
    @Override
    public void simpleUpdate(float tpf) {
        if (++frame < 5) {
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        }
    }
    @Override
    public void onAnalog(String name, float value, float tpf) {
        float moveSpeed = 4f;
        switch (name) {
            case "x+": meshLight.move(moveSpeed * tpf, 0, 0); break;
            case "x-": meshLight.move(-moveSpeed * tpf, 0, 0); break;
            case "y+": meshLight.move(0, moveSpeed * tpf, 0); break;
            case "y-": meshLight.move(0, -moveSpeed * tpf, 0); break;
            case "z+": meshLight.move(0, 0, moveSpeed * tpf); break;
            case "z-": meshLight.move(0, 0, -moveSpeed * tpf); break;
        }
    }
    
}
