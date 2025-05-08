package codex.renthyl.sockets;

import java.util.Iterator;
import java.util.Stack;

public class UpstreamSocketIterator implements Iterable<Socket>, Iterator<Socket> {

    private final Stack<Iterator<? extends Socket>> iterators = new Stack<>();

    public UpstreamSocketIterator(Socket root) {
        iterators.push(root.upstreamIterator());
    }

    @Override
    public Iterator<Socket> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        while (!iterators.isEmpty()) {
            Iterator<? extends Socket> it = iterators.peek();
            if (it.hasNext()) {
                return true;
            }
            iterators.pop();
        }
        return false;
    }

    @Override
    public Socket next() {
        Socket s = iterators.peek().next();
        if (s != null) {
            Iterator<? extends Socket> it = s.upstreamIterator();
            if (it != null) {
                iterators.push(it);
            }
        }
        return s;
    }

}
