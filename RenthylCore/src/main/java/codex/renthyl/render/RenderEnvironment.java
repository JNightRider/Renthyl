package codex.renthyl.render;

import codex.renthyl.FrameGraphContext;

/**
 * Sets up the rendering context.
 */
public interface RenderEnvironment {

    /**
     * Environment that does not change any render settings.
     */
    RenderEnvironment DEFAULTS = new RenderEnvironment() {
        @Override
        public void applySettings(FrameGraphContext context) {}
        @Override
        public void restoreSettings(FrameGraphContext context) {}
    };

    /**
     * Applies setting changes.
     *
     * @param context
     */
    void applySettings(FrameGraphContext context);

    /**
     * Restores setting changes made by {@link #applySettings(FrameGraphContext)}.
     *
     * @param context
     */
    void restoreSettings(FrameGraphContext context);

}
