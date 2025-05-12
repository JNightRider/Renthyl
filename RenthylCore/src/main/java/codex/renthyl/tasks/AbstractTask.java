package codex.renthyl.tasks;

import codex.renthyl.render.RenderWorker;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.RenderingQueue;
import codex.renthyl.sockets.Socket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTask implements Renderable {

    private static final int UNQUEUED = -2, QUEUING = -1, QUEUED = 0;

    protected final Collection<Socket> sockets = new ArrayList<>();
    private final AtomicBoolean claimed = new AtomicBoolean(false);
    private boolean complete = false;
    private int position = UNQUEUED;

    @Override
    public void queue(RenderingQueue queue) {
        if (position < QUEUING) {
            // set flag immediately in anticipation of callbacks from sockets
            position = QUEUING;
            // queue upstream before queueing this
            queueSockets(queue);
            position = queue.add(this);
        }
    }

    @Override
    public void update(float tpf) {
        sockets.forEach(s -> s.update(tpf));
    }

    @Override
    public void prepare() {
        if (position < QUEUED) {
            throw new IllegalStateException("Task is being prepared, but is not properly queued.");
        }
        sockets.forEach(s -> s.reference(position));
    }

    @Override
    public boolean claim(RenderWorker worker) {
        return sockets.stream().allMatch(socket -> socket.isUpstreamAvailable(getPosition())) && !claimed.getAndSet(true);
    }

    @Override
    public void render() {
        // get resource from socket
        //Texture2D myTexture = mySocket.acquire();
        // do rendering stuff...
        renderTask();
        // release sockets
        sockets.forEach(s -> s.release(position));
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
        // reset sockets
        sockets.forEach(Socket::reset);
    }

    protected <T extends Socket> T addSocket(T socket) {
        sockets.add(socket);
        return socket;
    }

    protected void queueSockets(RenderingQueue queue) {
        for (Socket s : sockets) {
            s.queue(queue);
        }
    }

    protected void addSockets(Socket... sockets) {
        this.sockets.addAll(Arrays.asList(sockets));
    }

    protected void removeSocket(Socket socket) {
        sockets.remove(socket);
    }

    protected abstract void renderTask();

    public boolean isClaimed() {
        return claimed.get();
    }

    public int getPosition() {
        return position;
    }

}
