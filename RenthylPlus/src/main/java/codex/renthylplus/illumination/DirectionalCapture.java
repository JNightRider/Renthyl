package codex.renthylplus.illumination;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.GraphicsObject;
import codex.renthyl.definitions.FrameBufferDef;
import codex.renthyl.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.resources.ResourceWrapper;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.allocation.TemporalSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyljme.tasks.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingSphere;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.*;
import org.lwjgl.opengl.GL45;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Captures all scene color and depth layers from a particular direction as a 2D texture array.
 */
public class DirectionalCapture extends RasterTask {

    private final ResourceAllocator allocator;
    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final ArgumentSocket<Vector3f> direction = new ArgumentSocket<>(this);
    private final ArrayList<AllocatedRenderTarget> targets = new ArrayList<>();
    private final TemporalSocket<TextureArray> array;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final TextureDef<TextureArray> arrayDef = TextureDef.textureArray();
    private final GLComputeShader texArrayShader;
    private final WorkSize work = new WorkSize();
    private final CameraState camera;
    private GraphicsObject fragmentQuery;
    private TextureImage arrayImage;

    public DirectionalCapture(AssetManager assetManager, ResourceAllocator allocator, int width, int height) {
        this.allocator = allocator;
        addSockets(geometry, direction);
        array = addSocket(new TemporalSocket<>(this, allocator, arrayDef, 1));
        texArrayShader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/DepthPeeling/TextureArrayInsert.glsl");
        texArrayShader.uniformImage("TargetArray");
        texArrayShader.uniformsTexture("Color", "Depth");
        texArrayShader.uniformInt("Index");
        camera = new CameraState(new Camera(width, height), false);
        camera.getCamera().setParallelProjection(true);
    }

    @Override
    protected void renderTask() {
        final List<GeometryQueue> queues = geometry.acquire();
        fetchFragmentQuery();
        context.getFrameBuffer().push();
        positionCamera(queues);
        context.getCamera().pushValue(camera);
        peelScene(queues);
        context.getFrameBuffer().pop();
        context.getCamera().pop();
        writeTargetsToArray();
    }

    private GraphicsObject fetchFragmentQuery() {
        if (fragmentQuery == null) try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer i = stack.mallocInt(1);
            GL45.glGenQueries(i);
            fragmentQuery = GraphicsObject.create(i.get(0), GL45::glDeleteQueries);
        }
        return fragmentQuery;
    }

    private void positionCamera(Collection<GeometryQueue> queues) {
        Camera cam = camera.getCamera();
        cam.lookAtDirection(direction.acquireOrThrow("Direction required."), cam.getUp());
        BoundingSphere bound = new BoundingSphere();
        for (GeometryQueue q : queues) {
            for (Geometry g : q) {
                bound.mergeLocal(g.getWorldBound());
            }
        }
        float radius = bound.getRadius();
        cam.setLocation(bound.getCenter().subtract(cam.getDirection().mult(radius + cam.getFrustumNear())));
        cam.setFrustum(cam.getFrustumNear(), cam.getFrustumNear() + radius * 2f, radius, -radius, radius, -radius);
        cam.update();
        cam.updateViewProjection();
    }

    private void peelScene(Collection<GeometryQueue> queues) {
        int maxPasses = 50;
        int nextTarget = -1;
        do {
            GL45.glBeginQuery(GL45.GL_ANY_SAMPLES_PASSED, fragmentQuery.getId());
            if (++nextTarget >= targets.size()) {
                targets.add(new AllocatedRenderTarget());
            }
            AllocatedRenderTarget t = targets.get(nextTarget);
            t.fetch(position);
            context.getFrameBuffer().setValue(t.getFrameBuffer());
            context.clearBuffers();
            // raster scene
            for (GeometryQueue q : queues) {
                q.render(context);
            }
            GL45.glEndQuery(GL45.GL_ANY_SAMPLES_PASSED);
            if (--maxPasses == 0) { // ensure peeling doesn't recur indefinitely
                throw new RuntimeException("Depth peeling unbounded.");
            }
        } while (GL45.glGetQueryi(GL45.GL_ANY_SAMPLES_PASSED, fragmentQuery.getId()) != GL45.GL_FALSE);
        // remove targets that are unnecessary
        while (nextTarget >= targets.size()) {
            targets.removeLast(); // resources were never fetched this frame, so no need to release them
        }
    }

    private void writeTargetsToArray() {
        arrayDef.setLength(targets.size());
        if (arrayImage == null) {
            arrayImage = new TextureImage(array.getCurrent().acquire(), TextureImage.Access.WriteOnly);
        } else {
            arrayImage.setTexture(array.getCurrent().acquire());
        }
        int i = 0;
        for (AllocatedRenderTarget t : targets) {
            texArrayShader.set("Color", t.getColor());
            texArrayShader.set("Depth", t.getDepth());
            texArrayShader.set("Index", i++);
            texArrayShader.execute(work);
            t.release();
        }
    }

    private class AllocatedRenderTarget {

        private ResourceWrapper<FrameBuffer> frameBuffer;
        private ResourceWrapper<Texture2D> color;
        private ResourceWrapper<Texture2D> depth;

        public void fetch(int position) {
            color = ResourceWrapper.acquire(allocator, color, colorDef, position, position);
            depth = ResourceWrapper.acquire(allocator, depth, depthDef, position, position);
            bufferDef.setColorTarget(color.get());
            bufferDef.setDepthTarget(depth.get());
            frameBuffer = ResourceWrapper.acquire(allocator, frameBuffer, bufferDef, position, position);
        }

        public void release() {
            frameBuffer.release();
            color.release();
            depth.release();
        }

        public FrameBuffer getFrameBuffer() {
            return frameBuffer.get();
        }

        public Texture2D getColor() {
            return color.get();
        }

        public Texture2D getDepth() {
            return depth.get();
        }

    }

}
