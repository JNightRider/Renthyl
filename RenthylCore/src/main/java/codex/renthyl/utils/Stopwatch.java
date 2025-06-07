package codex.renthyl.utils;

import java.util.Stack;

public class Stopwatch {

    private static final Stack<Long> sessions = new Stack<>();
    private static int lastEndedSession = 0;

    public static void start(String task) {
        if (lastEndedSession < sessions.size()) {
            System.out.println("{");
        }
        System.out.print(indent() + task + ": ");
        sessions.push(System.nanoTime());
    }

    public static double stop() {
        long start = sessions.pop();
        long end = System.nanoTime();
        double millis = nanosToMillis(Math.abs(end - start));
        if (sessions.size() < lastEndedSession) {
            System.out.print(indent() + "}: ");
        }
        System.out.println(millis + "ms");
        lastEndedSession = sessions.size();
        return millis;
    }

    private static double nanosToMillis(long nanos) {
        return (double)nanos / 1_000_000;
    }

    private static String indent() {
        return "  ".repeat(sessions.size());
    }

}
