## 简介

redis 的 `set`、`get`、`del`命令都具有原子性，同时 redis 也适用于分布式存储。因此，`setnx`、`set` 常常用来作为分布式锁的实现。

`setnx` 经常在 redis-2.6.12 之前作为分布锁使用。redis-2.6.12 以后，`set` 支持了 `ex`、`nx` 参数，将 `setnx` 中死锁问题简单化。redis 官方推荐使用 `set` 来实现分布式锁。

## 基于 `setnx` 的分布式锁算法

这里介绍 `setnx` 的主要目的不是为了使用，而是介绍一些历史过程和分布式锁的一些常见的问题。

### 创建锁与释放锁
**创建**：`setnx`命令可以用于分布式锁的创建。当 key 已存在时，返回 0，表示创建失败，即锁获取失败；当 key 不存在时，命令返回为 1，表示创建成功，即锁获取成功。

```text
redis> SETNX mykey "Hello"
(integer) 1
redis> SETNX mykey "World"
(integer) 0
redis> GET mykey
"Hello"
redis> 
```

**释放**：锁成功创建后，通过 `del` 命令来释放锁。

### 死锁
按照上面的流程，C1(Client-1) 创建锁后，释放锁；C2 创建锁，释放锁.... 一切都按照预想情况运行着，直到有一次：

*   C3 创建锁，C3 由于某些原因，**锁释放失败**
*   C1 与 C2 进入了**死锁状态**

进入死锁状态的原因在于：**锁释放失败了，并且没有任何其他机制可以让锁失效**。因此，我们对锁做了改进：将锁的失效时间作为 value 存入 redis string value 中。如果 C3 再释放锁失败，还有 **失效时间** 保证锁可以被自动释放，这样就解决了以上问题。

```text
SETNX lock.foo <current Unix time + lock timeout + 1>
```

接下来要介绍当是，当 C3 没有在锁当有效期内释放时，C4与C5竞争锁的过程，这个过程借助了 `getset` 命令：
1. C4、C5 通过 `get` 获取锁过期时间，并同时判断锁已失效
2. C4、C5同时使用 `getset` 命令为失效的锁 `lock.foo` 设定一个新的 value，但是 C5 要比 C4 快一些
3. C5 通过 `getset` 的返回值得到 `lock.foo` 中存储的 Unix time，该时间要早于当前时间，因此 C5 获取锁成功
4. C4 通过 `getset` 的返回值也得到了 `lock.foo` 中存储的 Unix time，但是是刚刚 C5 设定的值，仍然处于有效期，C4 继续等待锁

尽管最终 `lock.foo` 中设定的 Unix time 是由 C4 设定，但是锁最终由 C5 争抢到。从本质而言，并没有破坏锁竞争的公平性，也没有造成再一次的死锁。

### 为什么不用 Redis expire 命令来控制锁的失效时间？

那是因为 `setnx` 和 `expire` 两个命令共同使用时，不具有原子性。所以存在这种情况：redis client 发送了 `setnx` 命令后宕机，导致 `expire` 命令没有执行，最终导致死锁问题出现。

>   通过 lua 脚本和 exec 命令也可以实现两命令的原子性执行。但这并不是本文的重点，不做赘述。

### 手动释放锁时，可能出现问题的场景
当 `getset` 和 Unix time 真正起作用之后，一切都将高枕无忧了。但是你还要注意的是，C3 的锁释放失败的原因是复杂的，C3 恢复后会不会立即开始释放锁呢？我们绝不能让这样的情况发生，因为此时 C3 很可能已经不再是锁的拥有者！

比较恰当的做法是，首先判断当前存入锁的 Unix time 是否与当初自己设定的 Unix time 相等，借此判断自己是否仍然持有锁。如果自己仍持有该锁，则使用 del 命令释放锁。

为保证这段逻辑的原子性，需要使用 `exec` 命令执行如下 lua 代码：

```lua
if redis.call("get",KEYS[1]) == ARGV[1]
then
    return redis.call("del",KEYS[1])
else
    return 0
end
```

> exec 命令在 redis-1.2.0 版本后支持

### 不具备可重入性
由于不能通过锁的任何信息（key、value）判断锁拥有者是不是本人，因此也无法进行重复加锁。基于以上算法实现对分布式锁 **不具有可重入性**。

> 注意，即使通过前缀的方式在 value 中指明了锁的拥有者，也可能会在锁失效后的竞争中变得不可预测。因为存在如下情况：C5 获取了锁，但是新的 value 却由 C4 设定。

## 基于 set 的分布式锁实现
redis-2.6.12 以后，`set` 支持了 `ex`、`nx` 参数，分布式锁的实现算法也变得简洁。

`setnx` 锁实现的复杂性在于 **对死锁情况对处理**，核心问题在于：实现 **定时锁（timeout lock）** 的过程十分曲折。`set` 通过 `ex` 参数解决了以上所有的问题，让定时锁实现变得非常简单。

```text
redis> set lock.foo a13f2ca23.1554550498 EX 60 NX
```

set 锁的实现有 2 点需要注意：

- 使用 **长随机字符串** 来作为 value，来标志锁请求的唯一性，确保锁的可重入性
- 使用 `del` 释放锁时，先通过 `get` 判断锁是否在有效期内，并且锁的持有者是否仍然是本人，避免误删。这一过程需要使用 `exec` 命令确保组合命令的原子性，过程与 `setnx` 的 `del` 操作相同

### 可重入性分析
由于 value 具有唯一性，可以确定锁持有者的身份，因此 **具有可重入性**。

## Demo

[Redis Distributed Lock Demo](https://github.com/TonyaBaSy/redis-demo/tree/master/distribute-lock) 中实现了以上两种分布式锁。

## 版权声明

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="知识共享许可协议" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>本作品采用<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">知识共享署名-非商业性使用 4.0 国际许可协议</a>进行许可。

<p align="center">
  <img src="assets/support.jpg" width="240px"/><br />感谢支持！
</p>