package codex.renthyl.render;

import codex.renthyl.FrameGraphContext;

public interface RenderEnvironment {

    RenderEnvironment DEFAULTS = new RenderEnvironment() {
        @Override
        public void applySettings(FrameGraphContext context) {}
        @Override
        public void restoreSettings(FrameGraphContext context) {}
    };

    void applySettings(FrameGraphContext context);

    void restoreSettings(FrameGraphContext context);

}
