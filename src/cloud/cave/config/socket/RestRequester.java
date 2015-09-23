package cloud.cave.config.socket;

import cloud.cave.service.IRestRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
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
    private boolean buggySupport;

    public RestRequester() {
        this.socketTimeout = 1200;
        this.connectionTimeout = 1200;
        this.buggySupport = false;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    @Override
    public void setBuggySupport(boolean buggySupport) {
        this.buggySupport = buggySupport;
    }

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException {
        logger.debug(String.format("Doing a get rest request to [%1$s] with socket timeout %2$s and connection timeout %3$s",
                url, socketTimeout, connectionTimeout));
        if (this.buggySupport)
            return doNastyRequest(url, params);
        else
            return doNiceRequest(url, params);
    }

    private String doNiceRequest(String url, List<NameValuePair> params) throws IOException {
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

    public String doNastyRequest(String url, List<NameValuePair> params) throws IOException {
        logger.debug(String.format("Doing a get rest request to [%1$s] with socket timeout %2$s and connection timeout %3$s",
                url, socketTimeout, connectionTimeout));
        HttpResponse response = Request.Get(url)
                .socketTimeout(socketTimeout)
                .connectTimeout(connectionTimeout)
                .execute()
                .returnResponse();

        String value = EntityUtils.toString(response.getEntity());;
        if (response.getStatusLine().getStatusCode() != 200
                && response.getStatusLine().getStatusCode() != 500
                && response.getStatusLine().getStatusCode() != 401) {
            throw new IOException(String.format("Error while getting the resource at [%1$s]. " +
                            "The resource returned the status code [%2$s] with the message [%3$s].",
                    url, response.getStatusLine().getStatusCode(), value));
        }

        return value;
    }
}
