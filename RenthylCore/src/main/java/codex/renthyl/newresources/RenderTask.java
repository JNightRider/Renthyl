package codex.renthyl.newresources;

import codex.renthyl.FGRenderContext;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class RenderTask implements Renderable {

    private static final int UNQUEUED = -2, QUEUING = -1, QUEUED = 0;

    protected final Collection<Socket> sockets;
    private final AtomicBoolean claimed = new AtomicBoolean(false);
    private boolean complete = false;
    private int position = -1;

    public RenderTask() {
        this(new ArrayList<>());
    }

    protected RenderTask(Collection<Socket> sockets) {
        this.sockets = sockets;
    }

    @Override
    public void update(float tpf) {}

    @Override
    public void prepare() {
        if (position < QUEUED) {
            throw new IllegalStateException("Task is being prepared, but is not properly queued.");
        }
        for (Socket s : sockets) {
            s.reference(position);
        }
    }

    @Override
    public void queue(RenderingQueue queue) {
        if (position < QUEUING) {
            // set flag immediately in anticipation of callbacks from sockets
            position = QUEUING;
            // queue upstream before queueing this
            for (Socket s : sockets) {
                s.queue(queue);
            }
            position = queue.add(this);
        }
    }

    @Override
    public boolean claim(RenderWorker worker) {
        return sockets.stream().allMatch(Socket::isUpstreamAvailable) && !claimed.getAndSet(true);
    }

    @Override
    public void render(FGRenderContext context) {
        // get resource from socket
        //Texture2D myTexture = mySocket.acquire();
        // do rendering stuff...
        execute(context);
        // release sockets
        sockets.forEach(Referenceable::release);
        // I feel this could be done better somehow, but not really a big deal
        complete = true;
    }

    @Override
    public boolean isRenderingComplete() {
        return complete;
    }

    @Override
    public void reset() {
        // reset flags
        claimed.set(false);
        complete = false;
        position = UNQUEUED;
        // reset all sockets
        for (Socket s : sockets) {
            s.reset();
        }
    }

    protected abstract void execute(FGRenderContext context);

    public boolean isClaimed() {
        return claimed.get();
    }

    public int getPosition() {
        return position;
    }

}
