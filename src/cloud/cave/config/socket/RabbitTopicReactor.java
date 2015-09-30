package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.domain.Region;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.common.ServerConfiguration;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mark on 9/29/15.
 */
public class RabbitTopicReactor extends RabbitReactor {
    private static final Logger logger = LoggerFactory.getLogger(RabbitTopicReactor.class);
    private ServerConfiguration config;
    private Region region;

    @Override
    public void initialize(Invoker invoker, ServerConfiguration config) {
        super.initialize(invoker, config);
        this.config = config;
    }

    @Override
    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public Region getRegion() {
        return this.region;
    }

    @Override
    public void run() {
        String loginQueue = "cave.login";
        String caveQueue = "cave."+this.region.toString();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.get(0).getHostName());
        factory.setPort(config.get(0).getPortNumber());
        factory.setHost("localhost");
        Connection connection;

        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(RabbitMQConfig.RPC_EXCHANGE_NAME, "topic");
            channel.queueDeclare(caveQueue, false, false, false, null);
            channel.queueDeclare(loginQueue, false, false, false, null);

            channel.queueBind(loginQueue, RabbitMQConfig.RPC_EXCHANGE_NAME, loginQueue);
            channel.queueBind(caveQueue, RabbitMQConfig.RPC_EXCHANGE_NAME, caveQueue);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(loginQueue, false, consumer);
            channel.basicConsume(caveQueue, false, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                BasicProperties props = delivery.getProperties();
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(props.getCorrelationId())
                        .build();

                String message = new String(delivery.getBody());
                logger.debug("--> AcceptED!");
                logger.debug("\u001b[1;33m--> Received " + message + "\u001b[0;37m");

                String response = readMessageAndReply(message);

                channel.basicPublish(RabbitMQConfig.RPC_EXCHANGE_NAME, props.getReplyTo(), replyProps, response.getBytes());
                logger.debug("\u001b[1;32m--< replied: " + response + "\u001b[0;37m");

                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                logger.debug("--< Acknowledged message handled");
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
