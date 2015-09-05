package cloud.cave.doubles;

import org.json.simple.JSONObject;

import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;

public class SaboteurCRHDecorator implements ClientRequestHandler {

    private ClientRequestHandler decoratee;
    private String exceptionMsg;

    public SaboteurCRHDecorator(ClientRequestHandler decoratee) {
        this.decoratee = decoratee;
        this.exceptionMsg = null;
    }

    public synchronized JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson)
            throws CaveIPCException {
        if (this.exceptionMsg != null) {
            throw new CaveIPCException(exceptionMsg, null);
        }
        return decoratee.sendRequestAndBlockUntilReply(requestJson);
    }

    public void throwNextTime(String caveIPCException) {
        this.exceptionMsg = caveIPCException;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        // Not relevant, as this request handler is only used in
        // testing and under programmatic control
    }
}
