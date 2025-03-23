package codex.renthylplus.gbuffer;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.draw.RenderMode;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;
import codex.renthyl.resources.tickets.TicketGroup;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

public class GBufferPass extends RenderPass {

    private ResourceTicket<GeometryQueue> geometry;
    private GBufferGroup gbuffers;

    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
    }
    @Override
    protected TicketGroup<?> createMainOutputGroup(String name) {
        return gbuffers = new GBufferGroup(name);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        gbuffers.declareAll(resources, this);
        reserve(gbuffers);
        reference(geometry);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        gbuffers.acquireRenderTargets(resources, fb);
        context.registerMode(RenderMode.frameBuffer(fb));
        resources.acquire(geometry).render(context, GeometryRenderHandler.DEFAULT);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}

    public void addBuffer(String name, TextureDef<Texture2D> def) {
        gbuffers.add(name, def);
    }
    public void removeBuffer(String name) {
        gbuffers.remove(name);
    }

}
