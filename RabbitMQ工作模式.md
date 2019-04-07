# RabbitMQ 工作模式

## 1. 简单队列 Simple Queue

![simple queue](./assets/python-one.png)

简单队列模式中，分别只包含唯一一个：消费者、生产者、消息队列，用于 Producer 和 Consumer 之间解耦。



## 2. 工作队列 Work Queue

![work queue](assets/python-two.png)

工作队列模式又称为 **任务队列（Task Queue）**，队列负责进行任务的分发。因此，工作队列中可以包含一个或者多个 Consumer，负责接收并完成任务。该模式通常用于任务密集型场景，用于流量消峰。

### 消息持久化

rabbit 支持队列持久化和消息持久化，用于避免 rabbit 发生宕机的时候，发生消息的丢失。

队列持久化，即在队列声明的时候，将 durable 参数设置为 true：

```
boolean durable = true;
channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
```

消息的持久化，需要生产者在发送消息时，在 AMQP 中注明该消息需要持久化：

```
AMQP.BasicProperties messagePersistent = MessageProperties.PERSISTENT_TEXT_PLAIN;
channel.basicPublish("", QUEUE_NAME, messagePersistent, message.getBytes());
```

### 消息完整性

rabbit 的通讯基于 TCP，通过消费端发送 ack 响应信息，告知 rabbit server 消息已经成功被接收。

```
int prefetchCount = 2;
channel.basicQos(prefetchCount);
```

`basicQos` 为 rabbit 允许的最大未发送 ack 消息的数目。以上面代码为例，消费者如果存在 2 条没有 ack 认证的消息，rabbit server 将不再发送任何消息到该消费者，直到其中一条 ack 消息成功发送给 rabbit server 为止。以此保证消息不会发生大量的丢失。

>   如果所有的消费者均处于忙碌状态，即都没有及时的发送 ack 认证消息，rabbit 将会发送消息的挤压，挤压量超过了 rabbit server 的承受范围后，rabbit 会通过 TCP 窗口自动降低生产者的生产速度。因此需要对此做好监控，及时发现并解决。

消费端的 ack 认证有两种方式：自动认证和手动认证：

*   自动认证：即当消费者接受到消息后，rabbit client 框架自动发送 ack 消息
*   手动认证：即 ack 消息通过 rabbit client API 由开发人员手动控制发送的时机

```
// 自动 ack 认证
boolean autoAck = true;
channel.basicConsume(QUEUE_NAME, autoAck, callback, consumerTag -> { });

// 手动 ack 认证
DeliverCallback deliverCallback = (consumerTag, delivery) -> {
	String message = new String(delivery.getBody(), "UTF-8");

	System.out.println(" [x] Received '" + message + "'");
	try {
		doWork(message);
	} finally {
		System.out.println(" [x] Done");
		// 手动发送 ack 消息
		channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
	}
};
boolean autoAck = false;
channel.basicConsume(TASK_QUEUE_NAME, autoAck, deliverCallback, consumerTag -> { });
```

## 版权声明

<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="知识共享许可协议" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a>本作品采用<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">知识共享署名-非商业性使用 4.0 国际许可协议</a>进行许可。

<p align="center">
  <img src="assets/support.jpg" width="240px"/><br />感谢支持！
</p>