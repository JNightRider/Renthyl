package codex.renthyl.draw;

import codex.renthyl.FGRenderContext;
import codex.renthyl.util.GeometryRenderHandler;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;

public interface RenderEnv extends GeometryRenderHandler {

    void apply(FGRenderContext context);

    void reset(FGRenderContext context);

    class TestEnv implements RenderEnv {

        private Material material;
        private Material prevMat;

        @Override
        public void apply(FGRenderContext context) {
            prevMat = context.getRenderManager().getForcedMaterial();
            context.getRenderManager().setForcedMaterial(material);
        }

        @Override
        public void reset(FGRenderContext context) {
            context.getRenderManager().setForcedMaterial(prevMat);
        }

        @Override
        public void renderGeometry(FGRenderContext context, Geometry geometry) {

        }

    }

}
