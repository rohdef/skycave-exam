package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.ipc.CaveIPCException;
import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.server.common.ServerConfiguration;
import com.google.common.base.Strings;
import com.rabbitmq.client.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mark on 9/22/15.
 */
public class RabbitRequestHandler implements ClientRequestHandler {

    private static final Logger logger= LoggerFactory.getLogger(RabbitRequestHandler.class);
    private ServerConfiguration config;

    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson) throws CaveIPCException {
        return doSendRequestAndBlockUntilReply("", RabbitMQConfig.RPC_QUEUE_NAME, requestJson);
    }

    JSONObject doSendRequestAndBlockUntilReply(String exchangeName, String routingKey, JSONObject requestJson) throws CaveIPCException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.get(0).getHostName());
        factory.setPort(config.get(0).getPortNumber());
        Connection connection;
        String response = null;


        JSONObject replyJson = null;
        try {
            connection = factory.newConnection();
            Channel channel = connection.createChannel();
            String callbackQueueName = channel.queueDeclare().getQueue();
            if(!Strings.isNullOrEmpty(exchangeName)){
                channel.exchangeDeclare(exchangeName, "topic");
                channel.queueBind(callbackQueueName, exchangeName, callbackQueueName);
            }

            String corrId = java.util.UUID.randomUUID().toString();
            QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(callbackQueueName, true, consumer);
            String message = requestJson.toJSONString();

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .correlationId(corrId)
                    .replyTo(callbackQueueName)
                    .contentType("application/json")
                    .build();


            channel.basicPublish(exchangeName, routingKey,
                    props,
                    message.getBytes());

            while (true) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery();

                if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                    response = new String(delivery.getBody());
                    break;
                }
            }

            channel.close();
            connection.close();
            JSONParser parser = new JSONParser();
            replyJson = (JSONObject) parser.parse(response);
        } catch (IOException e) {
            logger.warn("", e);
        } catch (TimeoutException e) {
            logger.warn("", e);
        } catch (InterruptedException e) {
            logger.warn("", e);
        } catch (ParseException e) {
            logger.error("Did not return valid JSON, response was: " + response, e);
            throw new CaveIPCException("Did not return valid JSON, response was: " + response, e);

        } catch (NullPointerException e){
            logger.error("Unknown Nullpointer exception", e);
            throw new CaveIPCException("Unknown Nullpointer exception", e);
        } catch (Exception e){
            logger.error("", e);
            throw new CaveIPCException("Unknown critical error", e);
        }

        return replyJson;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
    }
}
