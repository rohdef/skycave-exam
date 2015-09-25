package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.ipc.CaveIPCException;
import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.server.common.ServerConfiguration;

import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.sun.corba.se.spi.activation._RepositoryStub;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.AMQP.*;

/**
 * Created by mark on 9/22/15.
 */
public class RabbitRequestHandler implements ClientRequestHandler{


    private ServerConfiguration config;

    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson) throws CaveIPCException {
        System.out.println("************************************************** Dumme GED **********************");
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("172.17.0.1");
        Connection connection = null;
        String response = null;

        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String callbackQueueName = channel.queueDeclare().getQueue();
            String corrId = java.util.UUID.randomUUID().toString();
            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(callbackQueueName, true, consumer);
            String message = requestJson.toJSONString();

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                            .correlationId(corrId)
                                            .replyTo(callbackQueueName)
                                            .contentType("application/json")
                                            .build();

            channel.basicPublish("", RabbitMQConfig.RPC_QUEUE_NAME,
                                                    props,
                                                    message.getBytes());


            while (true) {
                System.out.println("Unlimited repitition");
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                System.out.println("Delivery: " + new String(delivery.getBody()));

                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response = new String(delivery.getBody());
                    break;
                }
            }
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(response);

        return null;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
    }
}
