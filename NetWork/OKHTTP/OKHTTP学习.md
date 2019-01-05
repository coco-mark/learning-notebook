> ### 来自王老板的分享
> ---
> - #### OKHTTP系统架构模型
>   > ![OKHTTP系统架构模型（王召）](https://note.youdao.com/yws/api/personal/file/WEB9e4bbbf07a817a01ec22c1a1a31441ba?method=download&shareKey=8c44fb8fb19908f544c6b69c2cb092fe)
> - #### 线程池
>   > ![线程模型（王召）](https://note.youdao.com/yws/api/personal/file/WEB2ca932c063b86848d2f74bc7e947afe1?method=download&shareKey=58b16acfb566415f4584f2d2827b949e)
> - #### 连接池（ConnectionPool）
>   > ![连接池模型（王召）](https://note.youdao.com/yws/api/personal/file/WEB836d1ae35e85ab4395a0844d2898e1b5?method=download&shareKey=c580266e778718604830b1b5e0072e32)


> #### 思考
> ---
> - 概述
>   > - 如何创建
>   > - 如何管理
>   > - 如何回收
> - 要解决的问题
>   > - 连接池上限
>   > - 什么时候释放哪些连接
>   > - 有链接超时的情况


> #### 学习
> ---
> - 协议
>   > - HTTP 1.0
>   > - HTTP 1.1
>   > - HTTP 2
>   > - SPDY 3.1
>   > - QUIK (Quick UDP Internet Connection)
> - ##### 创建
>
>   >
>
> - ##### put新连接
>
>   > - 先检查空闲连接，将其清理
>   > - 放入新的连接
>   >
>   >   > ```java
>   >   > void put(RealConnection connection) {
>   >   >     assert (Thread.holdsLock(this));
>   >   >     if (!cleanupRunning) {
>   >   >        cleanupRunning = true;
>   >   >       executor.execute(cleanupRunnable);
>   >   >     }
>   >   >     connections.add(connection);
>   >   > }
>   >   > ```
>   >   >
>   >   > ```java
>   >   > private final Runnable cleanupRunnable = new Runnable() {
>   >   >     @Override
>   >   >     public void run() {
>   >   >         //循环执行清理操作
>   >   >         while (true) {
>   >   >             //返回清理执行等待的纳秒数
>   >   >             long waitNanos = cleanup(System.nanoTime());
>   >   >             if (waitNanos == -1) return;
>   >   >             if (waitNanos > 0) {
>   >   >                 long waitMillis = waitNanos / 1000000L;
>   >   >                 waitNanos -= (waitMillis * 1000000L);
>   >   >                 synchronized (ConnectionPool.this) {
>   >   >                     try {
>   >   >                         ConnectionPool.this.wait(waitMillis, (int) waitNanos);
>   >   >                     } catch (InterruptedException ignored) {
>   >   >                     }
>   >   >                 }
>   >   >             }
>   >   >         }
>   >   >     }
>   >   > };
>   >   > ```
> - ##### 清理
>   > - 如何找到闲置的连接
>   >   > - 通过```pruneAndGetAllocationCount(connection, now)```判断当前连接是不是在用
>   >   > - 当```idleDurationNs```纳秒数超过```keepAliveDurationNs```或者```idleConnectionCount```超过```maxIdleConnections```时，直接将当前连接移除
>   >   > - 上面情况不满足时，当```idleConnectionCount > 0```返回允许等待的时间差值
>   >   > - 当```inUseConnectionCount > 0```返回keepAlive的最大时间
>   >   > - 当前无连接不需要清理
>   > - 如何判断连接是否在用
>   >
>   >    >- 主要检查```Reference```的```StreamAllocation```是否为空，为空则说明有连接泄漏，程序有异常，不为空则返回```Reference```的列表size