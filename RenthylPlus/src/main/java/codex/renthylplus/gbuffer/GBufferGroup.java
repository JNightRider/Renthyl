package codex.renthylplus.gbuffer;

import codex.renthyl.definitions.TextureDef;
import codex.renthyl.resources.ResourceList;
import codex.renthyl.resources.tickets.DefinedTicketList;
import codex.renthyl.resources.tickets.ResourceTicket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

public class GBufferGroup extends DefinedTicketList<Texture2D, TextureDef<Texture2D>> {

    public GBufferGroup(String name) {
        super(name);
    }

    public void acquireRenderTargets(ResourceList resources, FrameBuffer fbo) {
        int i = 0;
        boolean depthApplied = false;
        for (ResourceTicket<Texture2D> t : getTickets()) {
            TextureDef<Texture2D> def = getDef(t.getName());
            Texture2D tex = resources.acquire(t);
            if (!def.getFormat().isDepthFormat()) {
                if (i >= fbo.getNumColorTargets() || fbo.getColorTarget(i).getTexture() != tex) {
                    fbo.replaceColorTarget(i, FrameBuffer.FrameBufferTarget.newTarget(tex));
                    fbo.setUpdateNeeded();
                }
                i++;
            } else {
                if (depthApplied) {
                    throw new IllegalStateException("GBuffer contains multiple depth targets.");
                }
                if (fbo.getDepthTarget() == null || fbo.getDepthTarget().getTexture() != tex) {
                    fbo.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(tex));
                    fbo.setUpdateNeeded();
                }
                depthApplied = true;
            }
        }
    }

}
