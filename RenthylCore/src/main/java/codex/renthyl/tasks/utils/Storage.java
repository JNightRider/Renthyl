package codex.renthyl.tasks.utils;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyl.tasks.AbstractTask;

import java.util.HashMap;
import java.util.Map;

public class Storage <K, T> extends AbstractTask {

    private final HashMap<K, ArgumentSocket<T>> sources = new HashMap<>();
    private final HashMap<K, ValueSocket<T>> units = new HashMap<>();

    @Override
    public void preStage(GlobalAttributes globals) {
        if (!prestaged) {
            for (K key : sources.keySet()) {
                fetchUnit(key);
            }
        }
        super.preStage(globals);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);
        for (Socket<T> s : sources.values()) {
            s.update(tpf);
        }
    }

    @Override
    public void prepare() {
        super.prepare();
        for (Socket<T> s : sources.values()) {
            s.reference(position);
        }
    }

    @Override
    public boolean ready() {
        return sources.values().stream().allMatch(s -> s.isUpstreamAvailable(position)) && super.ready();
    }

    @Override
    public void reset() {
        super.reset();
        for (Socket<T> s : sources.values()) {
            s.resetSocket();
        }
        sources.clear();
    }

    @Override
    protected void renderTask() {
        for (Map.Entry<K, ArgumentSocket<T>> e : sources.entrySet()) {
            units.get(e.getKey()).setValue(e.getValue().acquire());
        }
    }

    @Override
    protected void stageSockets(GlobalAttributes globals, RenderingQueue queue) {
        super.stageSockets(globals, queue);
        for (Socket<T> s : sources.values()) {
            s.stage(globals, queue);
        }
    }

    public Socket<T> fetchUnit(K key) {
        return units.computeIfAbsent(key, k -> addSocket(new ValueSocket<>(this)));
    }

    public ArgumentSocket<T> fetchSource(K key) {
        return sources.computeIfAbsent(key, k -> new ArgumentSocket<>(this));
    }

    public ArgumentSocket<T> connectSource(K key, Socket<T> source) {
        ArgumentSocket<T> s = fetchSource(key);
        s.setUpstream(source);
        return s;
    }

    public int getNumUnits() {
        return units.size();
    }

    public int getNumSources() {
        return sources.size();
    }

}
