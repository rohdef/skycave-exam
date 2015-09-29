package cloud.cave.config.socket;

import cloud.cave.config.RabbitMQConfig;
import cloud.cave.ipc.CaveIPCException;
import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.server.common.ServerConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by mark on 9/29/15.
 */
public class RabbitTopicRequestHandler extends RabbitRequestHandler {


    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson) throws CaveIPCException {
        return doSendRequestAndBlockUntilReply(RabbitMQConfig.RPC_EXCHANGE_NAME, "cave.login", requestJson);
    }
}
