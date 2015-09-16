package cloud.cave.config.socket;

import cloud.cave.service.IRestRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class RestRequester implements IRestRequest {
    private static final Logger logger = LoggerFactory.getLogger(RestRequester.class);
    private int socketTimeout, connectionTimeout;

    public RestRequester() {
        this.socketTimeout = 1200;
        this.connectionTimeout = 1200;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException {
        logger.debug(String.format("Doing a get rest request to [%1$s] with socket timeout %2$s and connection timeout %3$s",
                url, socketTimeout, connectionTimeout));
        return Request
                .Get(url)
                .socketTimeout(socketTimeout)
                .connectTimeout(connectionTimeout)
                .execute()
                .returnContent()
                .asString();
    }
}
