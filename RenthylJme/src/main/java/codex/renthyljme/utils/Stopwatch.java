package codex.renthyljme.utils;

import java.io.PrintStream;
import java.util.Stack;

/**
 * Simple hierarchal profiling tool.
 *
 * @author codex
 */
public class Stopwatch {

    private static final Stack<Long> sessions = new Stack<>();
    private static PrintStream output = System.out;
    private static int lastEndedSession = 0;

    public static void start(String task) {
        if (lastEndedSession < sessions.size()) {
            output.println("{");
        }
        output.print(indent() + task + ": ");
        sessions.push(System.nanoTime());

    }

    public static double stop() {
        long start = sessions.pop();
        long end = System.nanoTime();
        double millis = nanosToMillis(Math.abs(end - start));
        if (sessions.size() < lastEndedSession) {
            output.print(indent() + "} ");
        }
        output.println(millis + "ms");
        lastEndedSession = sessions.size();
        return millis;
    }

    public static void setOutput(PrintStream output) {
        Stopwatch.output = output;
    }

    public static PrintStream getOutput() {
        return output;
    }

    private static double nanosToMillis(long nanos) {
        return (double)nanos / 1_000_000;
    }

    private static String indent() {
        return "  ".repeat(sessions.size());
    }

}
