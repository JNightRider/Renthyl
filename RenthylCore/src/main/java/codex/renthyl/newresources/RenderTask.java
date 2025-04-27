package codex.renthyl.newresources;

/**
 * An executable member of a FrameGraph that can be executed, and maintains
 * a list of sockets which can be connected to.
 */
public interface RenderTask extends Executable, Queueable {

    void update();

}
