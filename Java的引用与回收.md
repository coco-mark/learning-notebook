# Java 的引用与回收

Java 中引用类型中主要包含了：强引用、软引用、弱引用、虚引用（本篇不涉及）。

强引用：通过 `new` 创建的引用，被强引用指向的对象，**不会被 gc 回收**。

```java
Object ref = new Object();
```

软引用：通过 `SoftReference` 包裹的引用，**内存不足**时，如果没有强引用指向它则被 gc 回收。

```java
SoftReference weakRef = new SoftReference(new Object);
```

弱引用：通过 `WeakReference` 包裹的引用，**下一次 gc** 时，如果没有强引用指向它则被 gc 回收。

```java
WeakReference weakRef = new WeakReference(new Object);
```

## 软引用 & 弱引用的使用

软引用一般用于**可伸缩式缓存**，即缓存本身的大小不固定，可随着存储空间的增加而增加。因为软引用只有在**内存不足时，才会被 gc 回收**。

基于弱引用的回收特性，最常见的一种用法是 `WeakHashMap`。弱引用还可以解决 [Lapsed listener problem](https://en.wikipedia.org/wiki/Lapsed_listener_problem)。

当出现“内存坏账”的时候，它可以解决“坏账”对象的回收问题。例如，使用 `ThreadLocal` 容器时，可以将 `WeakReference` 作为元素，这样可以不用考虑被引用对象的回收问题。

### 何时被回收

当使用软引用或弱引用的时候，要明白创建了 2 个对象：**引用对象（Reference）**和**被引用对象（Referent）**。被引用对象根据约定会被 gc 回收。但是由于引用对象是强引用，不会被 gc 自动回收。

```java
// ref1 and object A
Reference ref1 = new WeakReference(new Object());
// ref2 and object B
Reference ref2 = new SoftReference(new Object());

// release refs by handle
if (ref1.get() == null) ref1 = null;
if (ref2.get() == null) ref2 = null;
```

其中，gc 会在约定下回收 `A`、`B` 两个对象，`ref1` 和 `ref2` 不会被 gc 回收。

如果不希望被引用对象 `A` 被 gc 回收，需要使用强引用 `N` 重新指向对象，让对象 `A` 处于**可达**状态。

```java
// ref1 and object A
Reference ref1 = new WeakReference(new Object());
Object N = ref1.get();
```

>  **可达性**是 JVM 在内存回收时，判断对象是否可以被回收的标准，更多关于内存回收的内容在 [JVM 内存回收](./JVM内存回收.md)

### ReferenceQueue

**引用队列（ReferenceQueue）** 可以和 Reference 配合使用。当 gc 会收了 Referent 后，会将 Reference 放入队列中，以此通知用户 Referent 已经被回收。

```java
ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
WeakReference<Object> ref = new WeakReference<>(new Object(), refQueue);
System.out.println(ref + " - " + ref.get());  // java.lang.ref.WeakReference@198e2867 - java.lang.Object@12f40c25
System.gc();
System.out.println(refQueue.remove()); // java.lang.ref.WeakReference@198e2867
```

## Tick, Tick

本篇重点讲解引用的回收：gc 回收的是 Referent，而不是 Reference。用于提醒读者，在后续的开发中，不要因使用了 Reference 却没有手动的清理 Reference 对象而出现内存泄漏。

## 扩展阅读

[JVM 内存回收](./JVM内存回收.md)

[Weak References in Java | Baeldung](https://www.baeldung.com/java-weak-reference)

[Soft References in Java | Baeldung](https://www.baeldung.com/java-soft-references)

[Lapsed listener problem | Wikipedia](https://en.wikipedia.org/wiki/Lapsed_listener_problem)

## 版权声明

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="知识共享许可协议" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>本作品采用<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">知识共享署名-非商业性使用 4.0 国际许可协议</a>进行许可。

<p align="center">
  <img src="assets/support.jpg" width="240px"/><br />感谢支持！
</p>



