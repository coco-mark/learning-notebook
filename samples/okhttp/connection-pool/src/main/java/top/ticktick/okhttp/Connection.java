package top.ticktick.okhttp;

import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cherry - 2019/1/1
 * @version 1.0.0
 */
public class Connection {

    public long idleNano;
    public final List<Reference<StreamAllocation>> allocations = new ArrayList<>();
    public final String host;

    public Connection(String host) {
        this.host = host;
    }
}
