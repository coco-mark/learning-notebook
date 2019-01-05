# Thread wait & sleep

## Thread wait

**线程等待（wait）** 是线程的状态之一。进入等待状态的线程会自动放弃 **对象锁（Monitor）**，然后进入线程等待状态。当其他线程调用 `notify()` 或 `notifyAll()` ，等待线程进入**可运行状态（Runnable）**，等待 CPU 调度。[线程的一生](./线程的一生.md)介绍了线程状态间切换的过程。

调用 `Object.wait()` 前，**必须**已经获取了对象锁，否则将抛出 [IllegalMonitorStateException](https://docs.oracle.com/javase/8/docs/api/java/lang/IllegalMonitorStateException.html)。

```java
public class Demo {
    private final Object lock = new Object();
    
    public void badUsage() {
        // will throw IllegalMonitorStateException
        lock.wait();
    }
    
    public void goodUsage() {
        synchronized (lock) {
            lock.wait();
        }
    }
}
```

## Thread sleep

处于 sleep 的线程仍然处于 **运行（Running）** 状态。与 wait 不同是：线程不会因为 sleep 而放弃对象锁。当然，在任何情况下都可以调用 `Thread.sleep()` 方法，即使是未获得任何对象锁的前提下。

处于 sleep 下的线程，可能被其他线程**中断（Interrupt）**，中断响应后将抛出 [InterruptedException](https://docs.oracle.com/javase/8/docs/api/java/lang/InterruptedException.html)。[何时需要线程中断](./何时需要线程中断)中介绍了更多中断的内容。

## Thread await

`wait()` 方法属于 `Object` 类，`await()` 方法属于 `Condition` 类。

两者都是需要在获取锁的前提下调用，调用成功后放弃锁。前者获取对象锁，后者获取显式锁（Java 中 `Lock` 的实现类）。

`Object.notify()` 随机唤醒一个等待线程，`Condition.signal()` 唤醒指定的等待线程。这是使用上最大的不同。

## 扩展阅读

[线程的一生](./线程的一生.md)

[何时需要线程中断](./何时需要线程中断)

[Java并发编程：线程间协作的两种方式：wait、notify、notifyAll和Condition - 海子](https://www.cnblogs.com/dolphin0520/p/3920385.html)

## 版权声明

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="知识共享许可协议" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>本作品采用<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">知识共享署名-非商业性使用 4.0 国际许可协议</a>进行许可。

<p align="center">
  <img src="assets/support.jpg" width="240px"/><br />感谢支持！
</p>
