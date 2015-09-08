package cloud.cave.config.socket;

import cloud.cave.service.IRestRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;

import java.io.IOException;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class RestRequester implements IRestRequest {
    private int socketTimeout, connectionTimeout;

    public RestRequester() {
        this.socketTimeout = 1000;
        this.connectionTimeout = 1000;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException {
        return Request
                .Get(url)
                .socketTimeout(socketTimeout)
                .connectTimeout(connectionTimeout)
                .execute()
                .returnContent()
                .asString();
    }
}
