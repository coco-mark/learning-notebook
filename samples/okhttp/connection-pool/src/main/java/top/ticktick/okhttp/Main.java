package top.ticktick.okhttp;

import org.apache.commons.lang3.RandomUtils;

/**
 * @author cherry - 2019/1/1
 * @version 1.0.0
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        final ConnectionPool connectionPool = new ConnectionPool();

        // start the task, which will find cnn and sleep Random(0, 6) seconds.
        // Finally task will release the cnn.
        final String host = "http://okhttp.ticktick.top";
        Runnable task = () -> {
            StreamAllocation stream = new StreamAllocation(connectionPool);
            Connection cnn = stream.findConnection(host);
            try {
                Thread.sleep(RandomUtils.nextInt(0, 6) * 1000);
            } catch (InterruptedException ignored) {

            } finally {
                stream.release(cnn);
            }
        };

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(task);
            t.start();
            Thread.sleep(RandomUtils.nextInt(0, 10));
        }
        Thread.sleep(12 * 1000);
    }
}
