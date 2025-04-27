package codex.renthyl.newresources;

import codex.renthyl.FGRenderContext;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public class BasicTask implements RenderTask {

    private final Collection<Socket> sockets = new ArrayList<>();
    private final Socket<Texture2D> mySocket = new BasicSocket<>(this);
    private final AtomicBoolean claimed = new AtomicBoolean(false);
    private boolean queued = false;
    private boolean complete = false;
    private int position = -1;

    public BasicTask() {
        sockets.add(mySocket);
    }

    @Override
    public void update() {
        // do updating before thingy
    }

    @Override
    public void prepare() {
        if (position < 0) {
            throw new IllegalStateException("Task is being prepared, but is not properly queued.");
        }
        for (Socket s : sockets) {
            s.reference(position);
        }
    }

    @Override
    public boolean ready() {
        return sockets.stream().allMatch(Socket::isUpstreamAvailable);
    }

    @Override
    public void queue(ExecutionQueue queue) {
        if (!queued) {
            // set flag immediately in anticipation of callbacks from sockets
            queued = true;
            // queue upstream before queueing this
            for (Socket s : sockets) {
                s.queue(queue);
            }
            position = queue.add(this);
        }
    }

    @Override
    public boolean claim(Worker worker) {
        return true;
    }

    @Override
    public void execute(FGRenderContext context) {
        // get resource from socket
        Texture2D myTexture = mySocket.acquire();
        // do rendering stuff...
        // release sockets
        sockets.forEach(Referenceable::release);
        // I feel this could be done better somehow, but not really a big deal
        complete = true;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

    @Override
    public void reset() {
        // reset flags
        queued = false;
        complete = false;
        position = -1;
        // reset all sockets
        for (Socket s : sockets) {
            s.reset();
        }
    }

}
