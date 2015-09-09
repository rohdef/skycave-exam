package cloud.cave.doubles;

import org.json.simple.JSONObject;

import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;

public class SaboteurCRHDecorator implements ClientRequestHandler {

    private ClientRequestHandler decoratee;
    private String exceptionMsg;
    private Exception innerException;

    public SaboteurCRHDecorator(ClientRequestHandler decoratee) {
        this.decoratee = decoratee;
        this.exceptionMsg = null;
        this.innerException = null;
    }

    public synchronized JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson)
            throws CaveIPCException {
        if (this.exceptionMsg != null) {
            throw new CaveIPCException(exceptionMsg, innerException);
        }
        return decoratee.sendRequestAndBlockUntilReply(requestJson);
    }

    public void throwNextTime(String caveIPCException) {
        this.exceptionMsg = caveIPCException;
    }

    public void throwNextTime(String caveIPCException, Exception innerException) {
        this.exceptionMsg = caveIPCException;
        this.innerException = innerException;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        // Not relevant, as this request handler is only used in
        // testing and under programmatic control
    }
}
