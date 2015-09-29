package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.ipc.Invoker;
import cloud.cave.ipc.Reactor;
import cloud.cave.server.common.ServerConfiguration;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mark on 9/29/15.
 */
public class RabbitTopicReactor extends RabbitReactor {

    private ServerConfiguration config;
    private Invoker invoker;

    @Override
    public void initialize(Invoker invoker, ServerConfiguration config) {
        super.initialize(invoker, config);
        this.config = config;
        this.invoker = invoker;
    }

    @Override
    public void run() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.get(0).getHostName());
        factory.setPort(config.get(0).getPortNumber());
        factory.setHost("localhost");
        Connection connection = null;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(RabbitMQConfig.RPC_EXCHANGE_NAME, "topic");
            String queueName = channel.queueDeclare().getQueue();

            String bindingKey = "cave.*";
            channel.queueBind(queueName, RabbitMQConfig.RPC_EXCHANGE_NAME, bindingKey);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(queueName, false, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                BasicProperties props = delivery.getProperties();
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(props.getCorrelationId())
                        .build();

                String message = new String(delivery.getBody());

                String response = readMessageAndReply(message);

                channel.basicPublish(RabbitMQConfig.RPC_EXCHANGE_NAME, props.getReplyTo(), replyProps, response.getBytes());
                System.out.println(response);

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
