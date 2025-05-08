package codex.renthyl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

public class FrameBufferManager {

    private final Collection<FrameBufferView> buffers = new ArrayList<>();

    public FBuffer getFrameBuffer(Object tag, int width, int height, int samples) {
        for (FrameBufferView v : buffers) {
            if (v.matches(tag, width, height, samples)) {
                return v.checkout();
            }
        }
        FrameBufferView v = new FrameBufferView(tag, width, height, samples, 1);
        buffers.add(v);
        return v.checkout();
    }

    public FBuffer getFrameBuffer(int width, int height, int samples) {
        return getFrameBuffer(null, width, height, samples);
    }

    public void flush() {
        for (Iterator<FrameBufferView> it = buffers.iterator(); it.hasNext();) {
            FrameBufferView v = it.next();
            if (!v.cycle()) {
                v.dispose();
                it.remove();
            }
        }
    }

    private static class FrameBufferView {

        private final Object tag;
        private final FBuffer frameBuffer;
        private int time, duration;

        public FrameBufferView(Object tag, int w, int h, int s, int duration) {
            this.tag = tag;
            this.frameBuffer = new FBuffer(w, h, s);
            this.time = this.duration = duration;
        }

        public boolean cycle() {
            return --time >= 0;
        }

        public boolean matches(Object tag, int w, int h, int s) {
            return frameBuffer.getWidth() == w && frameBuffer.getHeight() == h
                    && frameBuffer.getSamples() == s && Objects.equals(this.tag, tag);
        }

        public FBuffer checkout() {
            time = duration;
            return frameBuffer;
        }

        public void dispose() {
            frameBuffer.dispose();
        }

    }

}
