package me.asu.jobs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
public class SukProcess implements Runnable {

    String[]  cmds;
    OutThread thread;
    Process   process;
    int    exitCode = 0;
    Writer writer = null;
    volatile AtomicBoolean completed = new AtomicBoolean(false);

    public SukProcess(String... cmds) {
        this.cmds = cmds;

    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public void setOutput(Writer writer) {
        this.writer = writer;
    }

    @Override
    public void run() {
        ProcessBuilder builder = new ProcessBuilder(cmds);
        builder.redirectErrorStream(true);

        try {
            process = builder.start();
            try(InputStreamReader in = new InputStreamReader(process.getInputStream());
                BufferedReader inReader = new BufferedReader(in)) {
                thread = new OutThread(inReader);
                thread.start();
                // wait for the process to finish and check the exit code
                exitCode = process.waitFor();
                joinThread(thread);
                completed.set(true);
            }
        } catch (Exception e) {
            if (!completed.get()) {
                if (thread != null) {
                    thread.interrupt();
                    joinThread(thread);
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            wrapException(e);
        } finally {
            if (process!= null) process.destroy();
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // quietly
                }
            }
        }
    }

    public void wrapException(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }


    private static void joinThread(Thread t) {
        while (t.isAlive()) {
            try {
                t.join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                t.interrupt(); // propagate interrupt
            }
        }
    }

    class OutThread extends Thread {

        BufferedReader reader;

        public OutThread(BufferedReader reader) {
            super();
            setDaemon(true);
            this.reader = reader;
        }


        @Override
        public void run() {
            try {
                String line = reader.readLine();
                while ((line != null) && !isInterrupted()) {
                    line = reader.readLine();
                    if (writer == null) {
                        System.out.println(line);
                    } else {
                        writer.write(line);
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    ;
}
