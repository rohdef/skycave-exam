package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.domain.ThreadCrashExeption;
import cloud.cave.ipc.Invoker;
import cloud.cave.ipc.Marshaling;
import cloud.cave.ipc.Reactor;
import cloud.cave.ipc.StatusCode;
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
    private ServerConfiguration config;

    @Override
    public void initialize(Invoker invoker, ServerConfiguration config) {
        this.invoker = invoker;
        this.config = config;
    }

    @Override
    public void run() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(config.get(0).getHostName());
        connectionFactory.setPort(config.get(0).getPortNumber());

        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(RabbitMQConfig.RPC_QUEUE_NAME, false, false, false, null);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(RabbitMQConfig.RPC_QUEUE_NAME, false, consumer);
            logger.info("*** Connected to RabbitMQ ***");

            while (true){
                 try{
                    logger.debug("--> Accepting... ");
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    BasicProperties props = delivery.getProperties();
                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                                                            .Builder()
                                                            .correlationId(props.getCorrelationId())
                                                            .build();

                    String message = new String(delivery.getBody());

                    logger.debug("--> AcceptED!");

                    String reply = readMessageAndReply(message);

                    channel.basicPublish("", props.getReplyTo(), replyProps, reply.getBytes());
                    logger.debug("--< replied: " + reply);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    logger.debug("--< Acknowledged message handled");
                } catch (IOException e) {
                    logger.warn("Error receiving on RabbitReactor", e);
                    throw new RuntimeException("Error receiving on RabbitReactor", e);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted receiving on RabbitReactor", e);
                    throw new RuntimeException("Interrupted receiving on RabbitReactor", e);
                }catch (Exception e){
                    logger.error("Critical error detected while receiving", e);
                    throw new RuntimeException("Critical error detected while receiving", e);
                }
            }
        } catch (IOException e) {
            logger.warn("Error receiving on RabbitReactor", e);
            throw new ThreadCrashExeption("Error receiving on RabbitReactor", e);
        } catch (TimeoutException e) {
            logger.warn("Timeout receiving on RabbitReactor", e);
            throw new ThreadCrashExeption("Timeout receiving on RabbitReactor", e);
        } catch (Exception e){
            logger.error("Critical error detected while receiving", e);
            throw new RuntimeException("Critical error detected while receiving", e);
        }

    }

    String readMessageAndReply(String message){
        logger.debug("--> Received " +message);
        JSONObject messageJSON, reply;
        JSONParser parser = new JSONParser();

        try {
            messageJSON = (JSONObject) parser.parse(message);
            reply = invoker.handleRequest(messageJSON);
        } catch (ParseException e) {
            String errorMsg = "JSON Parse error on input: " + message;
            logger.warn(errorMsg, e);
            reply = Marshaling.createInvalidReplyWithExplantion(
                    StatusCode.SERVER_FAILURE, errorMsg);
        } catch (NullPointerException e) {
            String errorMsg = "NullPointeException when trying to JSON parse error the input: " + message;
            logger.warn(errorMsg, e);
            reply = Marshaling.createInvalidReplyWithExplantion(
                    StatusCode.SERVER_FAILURE, errorMsg);
        } catch (Exception e) {
            String errorMsg = "Error when JSON parsing the input: " + message;
            logger.warn(errorMsg, e);
            reply = Marshaling.createInvalidReplyWithExplantion(
                    StatusCode.SERVER_FAILURE, errorMsg);
        }
        if (reply == null) {
            String errorMsg = "The reply from the invoker was null";
            logger.error(errorMsg);
            reply = Marshaling.createInvalidReplyWithExplantion(
                    StatusCode.SERVER_FAILURE, errorMsg);
        }
        return reply.toJSONString();
    }
}
