# Thread wait & sleep

>   蛮荒之地，笔者正在火速开垦......

## Tick, Tick

本篇文档偏重于讲解 OkHttp 线程模型的“骨骼”，其中的细节没有过多的涉及。目的是了解连接池的核心——**安全高效的获取连接和回收连接**，这是最有“营养”的部分。关于 HTTP 的协议、代理、路由等，没有停留太久，这些不是本篇的重点。更多的介绍在 [HTTP 2.0 的价值在哪里](HTTP2.0的价值在哪里.md)。

照猫画虎，笔者仿照 OkHttp3 连接池模型写了一个 [Demo](./samples/okhttp/connectionpool/)，欢迎各位大牛探讨与斧正。

## 扩展阅读

[Thread wait & sleep](./Thread-wait-sleep.md)

[OkHttpClient3 架构简介](./OkHttpClient3架构简介)

[Java 的引用与回收](./Java的引用与回收.md)

[OkHttpClient3 线程模型](./OkHttpClient3线程模型.md)

[操作系统的线程管理](./操作系统的线程管理.md)

[HTTP 2.0 的价值在哪里](HTTP2.0的价值在哪里.md)

## 版权声明

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="知识共享许可协议" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>本作品采用<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">知识共享署名-非商业性使用 4.0 国际许可协议</a>进行许可。

<p align="center">
  <img src="assets/support.jpg" width="240px"/><br />感谢支持！
</p>