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
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static com.rabbitmq.client.AMQP.*;

/**
 * Created by mark on 9/22/15.
 */
public class RabbitRequestHandler implements ClientRequestHandler{

    private static final Logger logger= LoggerFactory.getLogger(RabbitRequestHandler.class);
    private ServerConfiguration config;

    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson) throws CaveIPCException {
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
