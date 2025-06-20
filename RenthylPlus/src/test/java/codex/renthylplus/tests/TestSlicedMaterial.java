package codex.renthylplus.tests;

import codex.boost.control.GeometryControl;
import codex.boost.material.ImmediateMatDef;
import codex.boost.material.ImmediateShader;
import codex.boost.mesh.NormalQuad;
import codex.jmecompute.opengl.GLRenderUtils;
import codex.renthyl.FrameGraph;
import codex.renthyljme.FrameGraphContext;
import codex.renthyl.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.render.RenderEnvironment;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyljme.sockets.macros.InputToggledMacro;
import codex.renthyl.sockets.macros.Macro;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.attributes.GlobalAccessor;
import codex.renthyljme.tasks.filter.FilterChain;
import codex.renthyljme.tasks.scene.ControlRenderPass;
import codex.renthyljme.tasks.scene.GeometryPass;
import codex.renthyljme.tasks.scene.OutputPass;
import codex.renthyljme.tasks.scene.SceneEnqueuePass;
import codex.renthylplus.SlicedMesh;
import codex.renthylplus.effects.ports.FXAAPass;
import codex.renthylplus.gbuffer.GBufferLightingPass;
import codex.renthylplus.gbuffer.GBufferPass;
import codex.renthylplus.lights.LightBufferPass;
import codex.renthylplus.lights.LightGatherPass;
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
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
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

    private Node scene;
    private boolean usingSliced = false;

    public static void main(String[] args) {
        TestSlicedMaterial app = new TestSlicedMaterial();
        AppSettings settings = new AppSettings(true);
        //settings.setFrameRate(60);
        settings.setWidth(768);
        settings.setHeight(768);
        //settings.setVSync(false);
        //settings.setGammaCorrection(false);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        //settings.setGraphicsDebug(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        renderer.setDefaultAnisotropicFilter(5);
        assetManager.registerLocator("", ImmediateShader.class);
        GLRenderUtils.initialize(this);
//        assetManager.registerLoader(LwjglAssetLoader.class,
//                "3ds", "3mf", "blend", "bvh", "dae", "fbx", "glb", "gltf",
//                "lwo", "meshxml", "mesh.xml", "obj", "ply", "stl");

        // profiler is causing an OpenGL error via glGetQueryObject
        DetailedProfilerState profiler = new DetailedProfilerState();
        stateManager.attach(profiler);

        flyCam.setDragToRotate(true);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        DirectionalLight dl = new DirectionalLight(new Vector3f(1f, -0.5f, 0f), new ColorRGBA(0.4f, 0.7f, 1f, 1f).mult(0.1f));
        rootNode.addLight(dl);

        viewPort.setBackgroundColor(ColorRGBA.Blue);
        viewPort.setPipeline(createPipeline(allocator, dl));
        flyCam.setMoveSpeed(10f);

        createScene();

        EnvironmentProbeControl env = new EnvironmentProbeControl(assetManager, 256);
        env.setPosition(new Vector3f(0f, 2.0f, 0f));
        //EnvironmentProbeControl.tagGlobal(rootNode);
        rootNode.addControl(env);
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(.5f)));
        env.rebake();

        //rootNode.addLight(assetManager.loadModel("Scenes/defaultProbe.j3o").getLocalLightList().get(0));

        Texture skyTex = assetManager.loadTexture(new TextureKey("Textures/HdrSnowMountains.jpg", true));
        Spatial sky = SkyFactory.createSky(assetManager, skyTex, SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);

        for (int i = 0; i < 10; i++) {
            PointLight l = new PointLight(new Vector3f(
                    FastMath.rand.nextFloat(-15f, 15f), 3f,
                    FastMath.rand.nextFloat(-15f, 15f)), FastMath.rand.nextFloat(8f, 15f));
            l.setColor(new ColorRGBA(1f, 1f, 0.8f, 1f));
            rootNode.addLight(l);
        }

        cam.setFov(80f);

        inputManager.addMapping("reload_scene", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, "reload_scene");

    }

    @Override
    public void simpleUpdate(float tpf) {
        //System.out.println("lights: " + rootNode.getLocalLightList().size());
    }

    private void createScene() {

        usingSliced = !usingSliced;

        if (scene != null) {
            scene.removeFromParent();
        }
        scene = new Node();
        rootNode.attachChild(scene);

        int extent = 20;
        float size = 10f;
        if (!usingSliced) {
            Node floors = new Node("floors");
            floors.setLocalTranslation(-extent * size * 0.5f, 0f, -extent * size * 0.5f);
            Material floorMat = createSlicedMaterial();
            Spatial detailedFloor = assetManager.loadModel("Models/brick_wall_displaced.gltf");
            Mesh slicedMesh = new SlicedMesh(new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, size, size, 0.5f, 0.5f), 1);
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
        } else {
            Mesh floorMesh = new SlicedMesh(new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, size * extent, size * extent, 0.5f, 0.5f), 1);
            floorMesh.scaleTextureCoordinates(new Vector2f(extent, extent));
            MikktspaceTangentGenerator.generate(floorMesh);
            Geometry largeFloor = new Geometry("large_floor", floorMesh);
            largeFloor.setMaterial(createSlicedMaterial());
            //largeFloor.setMaterial(createRegularMaterial());
            largeFloor.setShadowMode(RenderQueue.ShadowMode.Receive);
            //largeFloor.setQueueBucket(RenderQueue.Bucket.Transparent);
            largeFloor.addControl(new SortIdWatcher());
            scene.attachChild(largeFloor);
        }

        Spatial sofa;
        try {
            sofa = assetManager.loadModel("Models/Sofa/SofaReversiReplica001_Blender_Cycles.j3o");
            System.out.println("Model loaded from j3o");
        } catch (Exception ex) {
            sofa = assetManager.loadModel("Models/Sofa/SofaReversiReplica001_Blender_Cycles.gltf");
            System.out.println("Model loaded from gltf");
            try {
                BinaryExporter.getInstance().save(sofa, new File("/home/codex/java/projects/Renthyl/RenthylJme/src/test/resources/Models/Sofa/SofaReversiReplica001_Blender_Cycles.j3o"));
                System.out.println("  Model successfully cached to j3o");
            } catch (IOException e) {
                System.out.println("  Failed to cache model to j3o");
            }
        }
        sofa.setLocalScale(3f);
        sofa.setLocalTranslation(5f, 0f, 5f);
        sofa.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.PI * -1.1f, Vector3f.UNIT_Y));
        sofa.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        scene.attachChild(sofa);

    }

    private FrameGraph createPipeline(ResourceAllocator allocator, DirectionalLight dl) {

        FrameGraph fg = new FrameGraph(assetManager);

        // base tasks
        fg.addTask(new ControlRenderPass());
        OutputPass out = fg.addTask(new OutputPass());
        SocketFrame<InputToggledMacro<Integer>> renderMethodMacro = new SocketFrame<>(new InputToggledMacro<>(0, 1));

        // geoemtry queuing
        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        MapToList<String, GeometryQueue> queuesToList = new MapToList<>(new String[] {
                SceneEnqueuePass.OPAQUE, SceneEnqueuePass.SKY, SceneEnqueuePass.TRANSPARENT, SceneEnqueuePass.GUI, SceneEnqueuePass.TRANSLUCENT});

        // deferred pipeline
        LightGatherPass lightGather = new LightGatherPass();
        LightBufferPass lightBuffer = new LightBufferPass(allocator);
        GlobalAccessor<Camera> cameraGlobal = new GlobalAccessor<>(FrameGraphContext.CAMERA_GLOBAL);
        GBufferPass gbuffers = new GBufferPass(allocator);
        gbuffers.addBuffer(TextureDef.texture2D(Image.Format.RGBA32F));
        gbuffers.addBuffer(TextureDef.texture2D(Image.Format.RGBA32F));
        GBufferLightingPass gbufLighting = new GBufferLightingPass(assetManager, allocator);

        // forward pipeline
        GeometryPass geometry = new GeometryPass(allocator);
        geometry.getColorDef().setFormat(Image.Format.RGBA32F);
        geometry.getColorDef().setFormatFlexible(false);
        //NormalPass normals = new NormalPass(assetManager, allocator);

        // shadows
        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        shadows.addDirectionalLightSource(new Attribute<>(dl), 4096, 1);
        ShadowComposerPass composer = new ShadowComposerPass(assetManager, allocator);
        Derivative<Collection<ShadowMap>, ShadowMap> shadowMapExtractor = new Derivative<>() {
            @Override
            public ShadowMap apply(Collection<ShadowMap> shadowMaps) {
                return shadowMaps.stream().findFirst().orElseThrow();
            }
        };
        ShadowMapViewer shadowViewer = new ShadowMapViewer(allocator);

        // filters
        FilterChain filters = new FilterChain();
        ShadowInjectionPass shadowInject = filters.add(new ShadowInjectionPass(assetManager, allocator));
        shadowInject.getIntensity().setValue(0.65f);
        filters.add(new FXAAPass(assetManager, allocator));

        // pass lights
        lightBuffer.getLights().addCollectionSource(lightGather.getLights());
        gbufLighting.getLightData().setUpstream(lightBuffer.getLightData());

        // pass camera
        gbuffers.getCamera().setUpstream(cameraGlobal);
        gbufLighting.getCamera().setUpstream(gbuffers.getCamera()); // attach gbuffer lighting camera to gbuffer camera

        // pass geometry queues
        queuesToList.getMap().setUpstream(enqueue.getQueues());
        gbuffers.getGeometry().addCollectionSource(queuesToList.getList());
        geometry.getGeometry().addCollectionSource(queuesToList.getList());
        //normals.getGeometry().addCollectionSource(queuesToList.getList());

        // pass shadows
        shadows.getGeometry().addCollectionSource(queuesToList.getList());
        composer.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        shadowInject.getContribution().setUpstream(composer.getLightContribution());
        shadowInject.getNumLights().setUpstream(shadows.getNumLights());
        shadowMapExtractor.setUpstream(shadows.getShadowMaps());
        shadowViewer.getShadowMap().setUpstream(shadowMapExtractor);

        // pass gbuffers
        gbufLighting.getGbuffers().setUpstream(gbuffers.getGBuffers());
        gbufLighting.getDepth().setUpstream(gbuffers.getDepth());

        // pass color
        Multiplexor<Texture2D> colorMux = new Multiplexor<>();
        colorMux.getIndex().setUpstream(renderMethodMacro.get());
        colorMux.addUpstream(geometry.getOutColor()); // forward
        colorMux.addUpstream(gbufLighting.getResult()); // deferred

        // pass depth
        Multiplexor<Texture2D> depthMux = new Multiplexor<>();
        depthMux.getIndex().setUpstream(renderMethodMacro.get());
        depthMux.addUpstream(geometry.getOutDepth()); // forward
        depthMux.addUpstream(gbuffers.getDepth()); // gbuffer
        composer.getSceneDepth().setUpstream(depthMux); // pass muxed depth to shadows

        // pass normals
        Multiplexor<Texture2D> normalsMux = new Multiplexor<>();
        normalsMux.getIndex().setValue(-1);
        //depthMux.getIndex().setUpstream(renderMethodMacro.get());
        //normalsMux.addUpstream(normals.getOutColor()); // forward
        normalsMux.addUpstream(gbuffers.getGBuffers().get(1)); // gbuffer
        composer.getSceneNormals().setUpstream(normalsMux); // pass muxed normals to shadows

        // pass normals lambda
        composer.getReadNormalsLambda().setUpstream(getNormalsLambdaMux(renderMethodMacro.get()));

        // connect filters
        filters.getSceneColor().setUpstream(colorMux);
        filters.getSceneDepth().setUpstream(depthMux);

        // output channels
        InputToggledMux<Texture2D> outChannel = new InputToggledMux<>();
        outChannel.addUpstream(filters.getFilterResult());
        outChannel.addUpstream(colorMux);
        outChannel.addUpstream(depthMux);
        outChannel.addUpstream(normalsMux);
        outChannel.addUpstream(composer.getLightContribution());
        outChannel.addUpstream(shadowViewer.getResult());

        inputManager.addMapping("out_channel", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("render_method", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addListener(outChannel, "out_channel");
        inputManager.addListener(renderMethodMacro.get(), "render_method");

        out.getColor().setUpstream(outChannel);
        out.getDepth().setUpstream(depthMux);

        return fg;

    }

    private Multiplexor<String> getNormalsLambdaMux(Macro<Integer> renderMethod) {
        Multiplexor<String> mux = new Multiplexor<>() {
            @Override
            protected int getNextIndex(int index) {
                return getIndex().preview() - 1;
            }
        };
        mux.getIndex().setUpstream(renderMethod);
        mux.addUpstream(new Attribute<>(
                "vec2 norm = unpackHalf2x16(floatBitsToUint(texelFetch(normals, texel, 0).r));" +
                "return vec3(norm, sqrt(1.0 - norm.x*norm.x - norm.y*norm.y));"));
        return mux;
    }

    private Material createSlicedMaterial() {
        Material mat = new Material(assetManager, "RenthylJme/MatDefs/SlicedPBRLighting.j3md");
        Texture diffuse = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Diffuse.jpg", true));
        diffuse.setWrap(Texture.WrapMode.Repeat);
        diffuse.setMinFilter(Texture.MinFilter.Trilinear);
        mat.setTexture("BaseColorMap", diffuse);
        Texture roughness = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Roughness.jpg", true));
        roughness.setWrap(Texture.WrapMode.Repeat);
        roughness.setMinFilter(Texture.MinFilter.Trilinear);
        mat.setTexture("RoughnessMap", roughness);
        Texture normal = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Normal.png", true));
        normal.setMinFilter(Texture.MinFilter.Trilinear);
        normal.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("NormalMap", normal);
        Texture displacement = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Displacement.png", true));
        displacement.setMinFilter(Texture.MinFilter.Trilinear);
        displacement.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("DisplacementMap", displacement);
        Texture ao = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_AO.jpg", true));
        ao.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("LightMap", ao);
        mat.setBoolean("LightMapAsAOMap", true);
        mat.setFloat("NormalScale", 3.5f);
        mat.setFloat("Metallic", 0f);
        mat.setBoolean("UseSpecularAA", false);
        //mat.setBoolean("UseInstancing", true);
        //mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //mat.getAdditionalRenderState().setDepthTest(false);
        //mat.getAdditionalRenderState().setDepthWrite(false);
        mat.setBoolean("GenerateSlices", true);
        //mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        return mat;
    }

    private Material createRegularMaterial() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        Texture diffuse = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Diffuse.jpg", true));
        diffuse.setWrap(Texture.WrapMode.Repeat);
        diffuse.setMinFilter(Texture.MinFilter.Trilinear);
        mat.setTexture("BaseColorMap", diffuse);
        Texture roughness = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Roughness.jpg", true));
        roughness.setWrap(Texture.WrapMode.Repeat);
        roughness.setMinFilter(Texture.MinFilter.Trilinear);
        mat.setTexture("RoughnessMap", roughness);
        Texture normal = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_Normal.png", true));
        normal.setMinFilter(Texture.MinFilter.Trilinear);
        normal.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("NormalMap", normal);
        Texture ao = assetManager.loadTexture(new TextureKey("Textures/Brick/Brick_AO.jpg", true));
        ao.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("LightMap", ao);
        mat.setBoolean("LightMapAsAOMap", true);
        mat.setFloat("Metallic", 0f);
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

    private static class SortIdWatcher extends GeometryControl {

        private Integer lastSortId = null;

        @Override
        protected void controlUpdate(float tpf) {}

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            int id = geometry.getMaterial().getSortId();
            if (lastSortId != null && lastSortId != id) {
                System.out.println("Sort ID for " + geometry.getName() + " changed to " + id);
            }
            lastSortId = id;
        }

    }

}
