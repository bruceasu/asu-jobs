package me.asu.jobs;

public class SimpleLog {
    public static boolean VERBOSE = false;

    public static void error(String ...msgs) {
        if (msgs == null || msgs.length == 0) { return; }
        System.err.print("[ERROR] ");
        for (String msg : msgs) {
            System.err.print(msg);
            System.err.print(' ');
        }
        System.err.println();
    }

    public static void debug(String ...msgs) {
        if (msgs == null || msgs.length == 0) { return; }

        if (VERBOSE) {
            System.err.print("[DEBUG] ");
            for (String msg : msgs) {
                System.err.print(msg);
                System.err.print(' ');
            }
            System.err.println();
        }

    }

    public static void info(String ...msgs) {
        if (msgs == null || msgs.length == 0) {
            return;
        }
        System.out.print("[INFO] ");
        for (String msg : msgs) {
            System.out.print(msg);
            System.out.print(' ');
        }
        System.out.println();
    }
}
