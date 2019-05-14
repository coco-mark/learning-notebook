# [译] synchronization 与 Object Locking

作者：Thomas Kotzmann 和 Christian Wimmer

本文翻译自：[Synchronization and Object Locking](https://wiki.openjdk.java.net/display/HotSpot/Synchronization)

Java 语言的一个最重要的优势在于原生支持多线程编程。在多线程的场景中，通过加锁可以达到对共享对象同步访问的目的。Java 提供了原语来控制一些关键的代码区域，这些区域往往存在共享对象，同一时刻下只能允许一个线程执行。也就是说，第一个线程进入到该区域的时会 **加锁（lock）**。当第二个线程进入如果想进入，必须 **等待（Waiting）** 第一个线程 **解锁（unlock）**。

HotSpot™ VM 中，每一个对象的都有一个 **对象头（header word）**，位于对象的首部（译者注，类似于 HTTP 的 header）。对象头中包含了 hashcode、分代年龄、对象锁信息。对象头的数据结构和锁状态、状态转化关系如下图所示。

![](assets/Synchronization.gif)

上图的右边部分表示了一个标准加锁流程。只要对象没有加锁，最后两位一定为 01。当方法中使用 `synchronized(obj)` 加锁时，VM 会将 lock record 结构存储在当前 stack frame 中，lock record 中包含了 obj 的对象头以及指向 obj 的指针。然后，VM 将尝试使用 CAS（compare-and-swap） 操作将 lock record 的指针写入到 obj 的对象头中。如果成功了，当前线程则持有该锁。由于 lock record 总是与字边界保持一致，因此对象头的最后两位将设置为 00 表示对象已加锁。

如果由于对象本身已经加锁导致 CAS 操作失败了，VM 首先会检查对象头中存储的指针是否指向当前线程的 stack。如果成功表示当前对象已经拥有了锁，并且可以继续执行。对于这类递归加锁的对象，lock record 初始化时会存储 0，而不是对象头。只有当两个不同的线程并发对同一个对象加锁时，该轻量级锁将会膨胀成一个重量级锁，未竞争到锁的线程进入线程等待状态。

轻量级锁相比于重量级锁开销小，但是轻量级锁仍然无法规避多核处理器下为保证 CAS 原子化所执行带来的开销。并且大量数据证明，对象多次的加锁与解锁往往总是同一线程。Java 6 以后，该问题被 **偏向加锁技术（biased locking thchnique）** 解决了，偏向加锁技术基于 **无存储（store-free）** 实现的。所谓的无存储，即对象仅在第一次加锁的时候使用 CAS 操作将线程 ID 存储在对象头中。以后该线程再进行加锁和解锁，不需要任何 CAS 操作来完成存储动作。甚至 stack 中的 lock record 在写入时也不再需要初始化，因为在多次加锁前的检查时，不再需要检测 lock record 是否属于当前线程。（译者注，检查 mark word 中的 Thread ID 就可以知道锁的拥有线程）

当线程 B 执行 `synchronized(obj)` 操作，但是该 obj 已经偏向另外线程 A 时，偏向锁需要撤销（译者注，偏向锁的解锁不是线程主动触发的，而是由于锁竞争引起的）。VM 会遍历 B 线程的 stack，找出所有的 lock record，然后将其调整为轻量级锁，最后通过 CAS 将指向最开始的 lock record 的指针写入到对象头。在偏向锁撤销期间，所有的线程都需要挂起。当对象的 hashcode 被访问时，偏向锁也会被撤销。因为 hashcode 与 thread Id 在对象头中共享了同一块存储区域。

> 译者注，每次使用 `synchronized` 操作都会在 stack 中写入一条 lock record。最早的 lock record 即表示最外层的 `synchronized` 的那次加锁。撤销过程中，之所以开销很大，是因为 **撤销期间需要挂起所有的线程**

那些从设计之初就计划用来共享的对象的场景不适合使用偏向锁，例如：生产者/消费者模式中的队列对象。因此，一个类的实例频繁的撤销偏向锁，偏向锁会对该类（Class）不可用。这样的机制称之为 **批量撤销（bulk revocation）**。批量撤销机制开启后，该类的实例再进行加锁时，会直接使用标准的轻量级锁，该类新创建的对象也会被标记，自动加入到批量撤销机制的行列中。

与“批量撤销”异曲同工的机制是 **批量偏向（bulk rebiasing）**，该机制适用于一个类的对象虽然被不同的线程加锁或解锁，但是线程之间从来不会发生并发操作。该机制将偏向锁的偏向性失效，但是不会像“批量撤销”一样禁止偏向锁（译者注，偏向锁不会再偏向某一个线程，从而减少了 CAS 写入 threadId 的开销和撤销的开销）。它是基于类中一个自增时间戳实现的，每一次对类的实例加锁时，会将当前的时间戳写入到对象头中，从而使该对象锁偏向当前线程。

## 源码说明
同步影响了 JVM 很多模块：对象头的结构中定义了 oopDesc 和 markOopDesc（对象描述与标记描述）。轻量级锁的源码集中体现在解释器和编译器中，ObjectMonitor 代表了重量级锁，偏向锁主要是由 BiasedLocking 类实现的。偏向锁可以通过 `-XX:+UseBiasedLocking` 和 `-XX:-UseBiasedLocking` 来开启和关闭。Java 6 和 7 锁默认开启的，但是需要启动后过几秒钟才会被激活。因此，如果程序的执行时间很短暂，需要通过 `-XX:BiasedLockingStartupDelay=0` 来设定立即激活。
