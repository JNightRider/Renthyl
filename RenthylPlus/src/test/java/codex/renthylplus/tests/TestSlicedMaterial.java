package codex.renthylplus.tests;

import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.boost.mesh.NormalQuad;
import codex.renthyl.FrameGraph;
import codex.renthyl.FrameGraphContext;
import codex.renthyl.geometry.GeometryQueue;
import codex.renthyl.render.RenderEnvironment;
import codex.renthyl.resources.ResourceAllocationState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.filter.FilterChain;
import codex.renthyl.tasks.scene.ControlRenderPass;
import codex.renthyl.tasks.scene.GeometryPass;
import codex.renthyl.tasks.scene.OutputPass;
import codex.renthyl.tasks.scene.SceneEnqueuePass;
import codex.renthyl.tasks.utils.Derivative;
import codex.renthyl.tasks.utils.InputToggledMux;
import codex.renthyl.tasks.utils.MapToListPass;
import codex.renthylplus.SlicedMesh;
import codex.renthylplus.effects.ports.FXAAPass;
import codex.renthylplus.shadow.*;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class TestSlicedMaterial extends SimpleApplication implements ActionListener {

    private final int extent = 5;
    private final float size = 10f;
    private Node scene;
    private boolean usingSliced = false;

    public static void main(String[] args) {
        TestSlicedMaterial app = new TestSlicedMaterial();
        AppSettings settings = new AppSettings(true);
        //settings.setVSync(false);
        //settings.setFrameRate(0);
        settings.setWidth(768);
        settings.setHeight(768);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        renderer.setDefaultAnisotropicFilter(5);
        assetManager.registerLocator("", ImmediateShader.class);
//        assetManager.registerLoader(LwjglAssetLoader.class,
//                "3ds", "3mf", "blend", "bvh", "dae", "fbx", "glb", "gltf",
//                "lwo", "meshxml", "mesh.xml", "obj", "ply", "stl");

        DetailedProfilerState profiler = new DetailedProfilerState();
        stateManager.attach(profiler);
        flyCam.setDragToRotate(true);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        DirectionalLight dl = new DirectionalLight(new Vector3f(1f, -0.5f, 0f), new ColorRGBA(0.4f, 0.7f, 1f, 1f).mult(.2f));
        rootNode.addLight(dl);

        viewPort.setBackgroundColor(ColorRGBA.Blue);
        viewPort.setPipeline(createPipeline(allocator, dl));
        flyCam.setMoveSpeed(10f);

        createScene();

        EnvironmentProbeControl env = new EnvironmentProbeControl(assetManager, 256);
        //env.setColor(ColorRGBA.White.mult(0.1f));
        //env.getArea().setCenter(new Vector3f(0f, 7f, 0f));
        env.setPosition(new Vector3f(0f, 2.0f, 0f));
        //EnvironmentProbeControl.tagGlobal(rootNode);
        rootNode.addControl(env);
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(1f)));

        //rootNode.addLight(assetManager.loadModel("Scenes/defaultProbe.j3o").getLocalLightList().get(0));

        Texture skyTex = assetManager.loadTexture(new TextureKey("Textures/HdrSnowMountains.jpg", true));
        Spatial sky = SkyFactory.createSky(assetManager, skyTex, SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);

        cam.setFov(80f);

        inputManager.addMapping("reload_scene", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, "reload_scene");

    }

    private void createScene() {

        usingSliced = !usingSliced;

        if (scene != null) {
            scene.removeFromParent();
        }
        scene = new Node();
        rootNode.attachChild(scene);

        Node floors = new Node("floors");
        floors.setLocalTranslation(-extent * size * 0.5f, 0f, -extent * size * 0.5f);
        Material floorMat = createSlicedMaterial();
        Spatial detailedFloor = assetManager.loadModel("Models/brick_wall_displaced.gltf");
        Mesh slicedMesh = new SlicedMesh(new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, size, size, 0.5f, 0.5f), 16);
        for (int i = 0; i < extent; i++) {
            for (int j = 0; j < extent; j++) {
                Spatial floor = usingSliced ? new Geometry("floor", slicedMesh) : detailedFloor.clone();
                floor.setLocalTranslation(i * size, 0f, j * size);
                if (usingSliced) {
                    MikktspaceTangentGenerator.generate(floor);
                    floor.setMaterial(floorMat);
                } else {
                    floor.setMaterial(createRegularMaterial());
                }
                floor.setShadowMode(RenderQueue.ShadowMode.Receive);
                floors.attachChild(floor);
            }
        }
        scene.attachChild(floors);

//        Mesh floorMesh = new SlicedMesh(new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, size * extent, size * extent, 0.5f, 0.5f), 25);
//        floorMesh.scaleTextureCoordinates(new Vector2f(extent, extent));
//        MikktspaceTangentGenerator.generate(floorMesh);
//        Geometry largeFloor = new Geometry("large_floor", floorMesh);
//        largeFloor.setMaterial(createSlicedMaterial());
//        largeFloor.setShadowMode(RenderQueue.ShadowMode.Receive);
//        scene.attachChild(largeFloor);

        Spatial sofa;
        try {
            sofa = assetManager.loadModel("Models/Sofa/SofaReversiReplica001_Blender_Cycles.j3o");
            System.out.println("Model loaded from j3o");
        } catch (Exception ex) {
            sofa = assetManager.loadModel("Models/Sofa/SofaReversiReplica001_Blender_Cycles.gltf");
            System.out.println("Model loaded from gltf");
            try {
                BinaryExporter.getInstance().save(sofa, new File("/home/codex/java/projects/Renthyl/RenthylPlus/src/test/resources/Models/Sofa/SofaReversiReplica001_Blender_Cycles.j3o"));
                System.out.println("  Model successfully cached to j3o");
            } catch (IOException e) {
                System.out.println("  Failed to cache model to j3o");
            }
        }
        sofa.setLocalScale(3f);
        sofa.setLocalTranslation(5f, 0f, 5f);
        sofa.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI * -1.1f, Vector3f.UNIT_Y));
        sofa.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        //scene.attachChild(sofa);

        for (int i = 0; i < 10; i++) {
            rootNode.addLight(new PointLight(new Vector3f(
                    FastMath.rand.nextFloat(-15f, 15f), 3f,
                    FastMath.rand.nextFloat(-15f, 15f)), FastMath.rand.nextFloat(8f, 15f)));
        }

    }

    private FrameGraph createPipeline(ResourceAllocator allocator, DirectionalLight dl) {

        FrameGraph fg = new FrameGraph(assetManager);

        fg.addTask(new ControlRenderPass()).setContext(fg.getContext());
        OutputPass out = fg.addTask(new OutputPass());

        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        MapToListPass<String, GeometryQueue> mapToList = new MapToListPass<>(new String[] {
                SceneEnqueuePass.OPAQUE, SceneEnqueuePass.SKY, SceneEnqueuePass.TRANSPARENT, SceneEnqueuePass.GUI, SceneEnqueuePass.TRANSLUCENT});
        GeometryPass geometry = new GeometryPass(allocator);
        geometry.getColorDef().setFormat(Image.Format.RGBA32F);
        geometry.getColorDef().setFormatFlexible(false);
        NormalPass normals = new NormalPass(assetManager, allocator);
        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        shadows.addDirectionalLightSource(new Attribute<>(dl), 4096, 1);
        ShadowComposerPass composer = new ShadowComposerPass(assetManager, allocator);
        FilterChain filters = new FilterChain();
        ShadowInjectionPass shadowInject = filters.add(new ShadowInjectionPass(assetManager, allocator));
        shadowInject.getIntensity().setValue(0.65f);
        filters.add(new FXAAPass(assetManager, allocator));
        Derivative<Collection<ShadowMap>, ShadowMap> shadowMapExtractor = new Derivative<>() {
            @Override
            public ShadowMap apply(Collection<ShadowMap> shadowMaps) {
                return shadowMaps.stream().findFirst().orElseThrow();
            }
        };
        ShadowMapViewer shadowViewer = new ShadowMapViewer(allocator);

        mapToList.getMap().setUpstream(enqueue.getQueues());
        geometry.getGeometry().addCollectionSource(mapToList.getList());
        normals.getGeometry().addCollectionSource(mapToList.getList());

        shadows.getGeometry().addCollectionSource(mapToList.getList());
        composer.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        composer.getSceneDepth().setUpstream(geometry.getOutDepth());
        composer.getSceneNormals().setUpstream(normals.getOutColor());
        shadowInject.getContribution().setUpstream(composer.getLightContribution());
        shadowInject.getNumLights().setUpstream(shadows.getNumLights());
        shadowMapExtractor.setUpstream(shadows.getShadowMaps());
        shadowViewer.getShadowMap().setUpstream(shadowMapExtractor);

        filters.getSceneColor().setUpstream(geometry.getOutColor());
        filters.getSceneDepth().setUpstream(geometry.getOutDepth());

        InputToggledMux<Texture2D> outChannel = new InputToggledMux<>();
        outChannel.addUpstream(filters.getFilterResult());
        outChannel.addUpstream(geometry.getOutColor());
        outChannel.addUpstream(composer.getLightContribution());
        outChannel.addUpstream(shadowViewer.getResult());
        inputManager.addMapping("out_channel", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(outChannel, "out_channel");

        out.getColor().setUpstream(outChannel);
        out.getDepth().setUpstream(geometry.getOutDepth());

        return fg;

    }

    private Material createSlicedMaterial() {
        Material mat = new Material(assetManager, "RenthylPlus/MatDefs/SlicedPBRLighting.j3md");
        Texture diffuse = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Diffuse.jpg", true));
        diffuse.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("BaseColorMap", diffuse);
        Texture roughness = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Roughness.jpg", true));
        roughness.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("RoughnessMap", roughness);
        Texture normal = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Normal.png", true));
        normal.setMinFilter(Texture.MinFilter.Trilinear);
        normal.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("NormalMap", normal);
        Texture displacement = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Displacement.png", true));
        //displacement.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        displacement.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("DisplacementMap", displacement);
        Texture ao = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_AO.jpg", true));
        ao.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("LightMap", ao);
        mat.setBoolean("LightMapAsAOMap", true);
        mat.setFloat("NormalScale", 3.5f);
        mat.setFloat("Metallic", 0f);
        mat.setBoolean("GenerateSlices", false);
        mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        return mat;
    }

    private Material createRegularMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        mat.setTexture("BaseColorMap", assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Diffuse.jpg", false)));
        //sliced.setTexture("MetallicMap", assetManager.loadTexture("Textures/Brick/Brick_Metallic.jpg"));
        //mat.setTexture("RoughnessMap", assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Roughness.jpg", false)));
        //Texture normal = assetManager.loadTexture("Textures/Brick/Brick_Normal.png");
        //normal.setMinFilter(Texture.MinFilter.Trilinear);
        //mat.setTexture("NormalMap", normal);
        mat.setFloat("Metallic", 0f);
        Texture ao = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_AO.jpg", true));
        ao.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("LightMap", ao);
        mat.setBoolean("LightMapAsAOMap", true);
        return mat;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("reload_scene") && isPressed) {
            createScene();
        }
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
                matdef.createTechnique()
                        .setVersions(450, 310, 150)
                        .setShader(vert).setShader(frag)
                        .addWorldParameters("WorldViewProjectionMatrix", "WorldNormalMatrix")
                        .add();
            }
            material = matdef.createMaterial();
        }

        @Override
        public void applySettings(FrameGraphContext context) {
            //context.getForcedTechnique().pushValue("PreNormalPass");
            context.getForcedMaterial().pushValue(material);
        }

        @Override
        public void restoreSettings(FrameGraphContext context) {
            //context.getForcedTechnique().pop();
            context.getForcedMaterial().pop();
        }

    }

}
