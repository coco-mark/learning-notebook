package top.ticktick.okhttp;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * @author cherry - 2019/1/1
 * @version 1.0.0
 */
public class StreamAllocation {
    private Connection cnn;
    private final ConnectionPool connectionPool;

    public StreamAllocation(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public void release(Connection cnn) {
        for (int i = 0; i < cnn.allocations.size(); i++) {
            Reference<StreamAllocation> ref = cnn.allocations.get(i);
            if (ref.get() == this) {
                Reference<StreamAllocation> refRemoved = cnn.allocations.remove(i);
                System.out.printf("release stream, ref: %s, stream: %s\n", refRemoved, refRemoved.get());
                break;
            }
        }
        if (cnn.allocations.isEmpty()) {
            cnn.idleNano = System.nanoTime();
            System.out.printf("idle cnn: %s, idleNano: %d\n", cnn, cnn.idleNano);
        }
    }

    public Connection findConnection(String host) {
        System.out.printf("finding cnn, stream: %s, host: %s\n", this, host);
        if (cnn != null) {
            System.out.printf("found cnn from stream: %s\n", cnn);
            return cnn;
        }
        synchronized (this) {
            Connection cnn = connectionPool.get(host);
            if (cnn == null) {
                cnn = new Connection(host);
                System.out.println("create new cnn: " + cnn);
                connectionPool.put(cnn);
                acquire(cnn);
            } else {
                acquire(cnn);
                System.out.printf("found cnn from connection pool: %s, cnn.allocations: %d\n", cnn, cnn.allocations.size());
            }
            return cnn;
        }
    }

    private void acquire(Connection cnn) {
        assert (Thread.holdsLock(connectionPool));

        this.cnn = cnn;
        cnn.allocations.add(new WeakReference<>(this));
    }
}
