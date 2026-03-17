package me.asu.jobs;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JulLevels {
    public static void setLevel(String loggerName, Level level) {
        Logger logger = Logger.getLogger(loggerName);
        logger.setLevel(level);
    }

    public static void setRootLevel(Level level) {
        Logger root = Logger.getLogger("");
        root.setLevel(level);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(level);
        }
    }
}