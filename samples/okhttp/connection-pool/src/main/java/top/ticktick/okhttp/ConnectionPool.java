package top.ticktick.okhttp;

import java.lang.ref.Reference;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author cherry - 2019/1/1
 * @version 1.0.0
 */
public class ConnectionPool {

    private final Deque<Connection> connections = new ArrayDeque<>();
    private ExecutorService executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5, TimeUnit.MINUTES, new SynchronousQueue<>(), Util.threadFactory("cleanup-thread", true));
    private boolean cleanupRunning;
    private final long MAX_IDLE_NANO = TimeUnit.SECONDS.toNanos(3);
    private final int MAX_IDLE_SIZE = 3;
    private final int MAX_ALLOCATION_COUNT = 3;

    private Runnable cleanupRunnable = () -> {
        while (true) {
            long now = System.nanoTime();
            System.out.printf("start cleanup in %d.\n", now);
            long waitNano = cleanup(now);
            System.out.printf("finished cleanup, use: %dns, will wait: %dns\n", System.nanoTime() - now, waitNano);
            if (waitNano == -1) {
                System.out.println("cleanup runnable closed.");
                return;
            }
            if (waitNano > 0) {
                long waitMill = waitNano / 1000000;
                waitNano = waitNano - waitMill * 1000000;
                synchronized (this) {
                    try {
                        ConnectionPool.this.wait(waitMill, (int) waitNano);
                    } catch (InterruptedException ignore) {

                    }
                }
            }
        }
    };

    /**
     * 获取连接
     * @param host
     * @return
     */
    public Connection get(String host) {
        for (Connection cnn : connections) {
            if (isEligible(cnn, host)) {
                return cnn;
            }
        }
        return null;
    }

    /**
     * 加入新的连接
     * @param cnn
     */
    public void put(Connection cnn) {
        assert cnn != null;
        // 有新的 cnn 加入，开启清理线程
        if (!cleanupRunning) {
            cleanupRunning = true;
            executor.execute(cleanupRunnable);
            System.out.println("cleanup runnable stated.");
        }
        connections.add(cnn);
    }

    boolean isEligible(Connection cnn, String host) {
        return cnn.host.equals(host) && cnn.allocations.size() < MAX_ALLOCATION_COUNT;
    }

    long cleanup(long now) {
        int inUseCount = 0;
        int idleCount = 0;
        long longestIdleDurationNs = -1;
        Connection longestIdleCnn = null;

        synchronized (this) {
            Iterator<Connection> it = connections.iterator();
            while (it.hasNext()) {
                Connection cnn = it.next();
                if (pruneAndGetAllocationCount(cnn, now) > 0) {
                    inUseCount++;
                    continue;
                }
                idleCount++;
                long waitNano = now - cnn.idleNano;
                if (waitNano > longestIdleDurationNs) {
                    longestIdleDurationNs = waitNano;
                    longestIdleCnn = cnn;
                }
            }
            if (longestIdleDurationNs > MAX_IDLE_NANO ||
                    idleCount > MAX_IDLE_SIZE) {
                connections.remove(longestIdleCnn);
                System.out.printf("clean cnn: %s, longestIdleNanos: %d, idleCount: %d\n", longestIdleCnn, longestIdleDurationNs, idleCount);
                return 0;
            }
            if (idleCount > 0) {
                return MAX_IDLE_NANO - longestIdleDurationNs;
            }
            // all in use
            if (inUseCount > 0) {
                return MAX_IDLE_NANO;
            }

            cleanupRunning = false;
            return -1;
        }
    }

    public int pruneAndGetAllocationCount(Connection cnn, long now) {
        List<Reference<StreamAllocation>> refs = cnn.allocations;
        for (int i = 0; i < refs.size(); ) {
            Reference<StreamAllocation> ref = refs.get(i);
            if (ref.get() != null) {
                i++;
                continue;
            }

            // memory lack
            System.err.printf("memory lack, cnn not release correctly, cnn: %s, ref: %s\n", cnn, ref);
            refs.remove(i);

            // If this was the last allocation, the connection is eligible for immediate eviction.
            if (refs.isEmpty()) {
                cnn.idleNano = now - MAX_IDLE_NANO;
                return 0;
            }
        }
        return refs.size();
    }
}
