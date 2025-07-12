package codex.renthyljme;

import codex.renthyl.FrameGraph;
import codex.renthyl.render.queue.RenderingQueue;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.pipeline.RenderPipeline;

import java.util.concurrent.Executor;

public class JmeFrameGraph extends FrameGraph implements RenderPipeline<FrameGraphContext> {

    protected final AssetManager assetManager;
    private boolean rendered = false;

    public JmeFrameGraph(AssetManager assetManager) {
        super();
        this.assetManager = assetManager;
    }

    public JmeFrameGraph(AssetManager assetManager, Executor service) {
        super(service);
        this.assetManager = assetManager;
    }

    public JmeFrameGraph(AssetManager assetManager, RenderingQueue queue) {
        super(queue);
        this.assetManager = assetManager;
    }

    @Override
    public FrameGraphContext fetchPipelineContext(RenderManager renderManager) {
        return renderManager.getOrCreateContext(FrameGraphContext.class, rm -> new FrameGraphContext(assetManager, rm));
    }

    @Override
    public void startRenderFrame(RenderManager rm) {}

    @Override
    public void pipelineRender(RenderManager rm, FrameGraphContext pContext, ViewPort vp, float tpf) {
        pContext.target(vp, tpf);
        render(pContext.getGlobals(), tpf);
        rm.getRenderer().clearClipRect();
        pContext.resetAllRenderSettings();
        rendered = true;
    }

    @Override
    public boolean hasRenderedThisFrame() {
        return rendered;
    }

    @Override
    public void endRenderFrame(RenderManager rm) {
        rendered = false;
    }

}
