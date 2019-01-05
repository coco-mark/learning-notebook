package top.ticktick.okhttp;

import java.util.concurrent.ThreadFactory;

/**
 * @author cherry - 2019/1/1
 * @version 1.0.0
 */
public class Util {

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return runnable -> {
            Thread result = new Thread(runnable, name);
            result.setDaemon(daemon);
            return result;
        };
    }
}
