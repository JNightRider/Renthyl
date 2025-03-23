package codex.renthyl.modules.geometry;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.draw.RenderEnvironment;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.ResourceTicket;

public class BaseEnvironmentPass extends RenderPass {

    private ResourceTicket<RenderEnvironment> environment;

    @Override
    protected void initialize(FrameGraph frameGraph) {
        environment = addOutput("Environment");
    }
    @Override
    protected void prepare(FGRenderContext context) {

    }
    @Override
    protected void execute(FGRenderContext context) {

    }
    @Override
    protected void reset(FGRenderContext context) {

    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {

    }

}
