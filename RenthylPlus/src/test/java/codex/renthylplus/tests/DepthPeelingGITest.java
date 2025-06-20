package codex.renthylplus.tests;

import codex.renthyl.FrameGraph;
import codex.renthyljme.tasks.scene.ControlRenderPass;
import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

public class DepthPeelingGITest extends SimpleApplication {

    public static void main(String[] args) {
        DepthPeelingGITest app = new DepthPeelingGITest();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        FrameGraph fg = new FrameGraph(assetManager);
        viewPort.setPipeline(fg);

        fg.add(new ControlRenderPass());

        DirectionalCapture

    }

}
