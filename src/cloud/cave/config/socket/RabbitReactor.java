package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.ipc.Invoker;
import cloud.cave.ipc.Reactor;
import cloud.cave.server.common.ServerConfiguration;
import com.mongodb.util.JSON;
import com.rabbitmq.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.content.audio.basic;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mark on 9/22/15.
 */
public class RabbitReactor implements Reactor {
    private static final Logger logger = LoggerFactory.getLogger(RabbitReactor.class);
    private Invoker invoker;

    @Override
    public void initialize(Invoker invoker, ServerConfiguration config) {
        this.invoker = invoker;

    }

    @Override
    public void run() {

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("172.17.0.1");
        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(RabbitMQConfig.RPC_QUEUE_NAME, false, false, false, null);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(RabbitMQConfig.RPC_QUEUE_NAME, false, consumer);
            logger.info("*** Connected to RabbitMQ ***");

            while (true){
                logger.debug("Accepting ");
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                BasicProperties props = delivery.getProperties();
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                                        .Builder()
                                                        .correlationId(props.getCorrelationId())
                                                        .build();

                String message = new String(delivery.getBody());
                System.out.println("Message recived: ");
                System.out.println(message);

                channel.basicPublish("", props.getReplyTo(), replyProps, message.getBytes());
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reciving on RabbitReactor", e);
        } catch (TimeoutException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reciving on RabbitReactor", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error reciving on RabbitReactor", e);
        }

    }

    private void readMessageAndReply(String message){
        JSONObject messageJSON, reply;
        JSONParser parser = new JSONParser();

        try {
            messageJSON = (JSONObject) parser.parse(message);
            reply = invoker.handleRequest(messageJSON);
        } catch (ParseException e) {

            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }


    }
}
