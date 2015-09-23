package cloud.cave.service;

import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public interface IRestRequest {
    void setBuggySupport(boolean buggySupport);

    String doRequest(String url, List<NameValuePair> params) throws IOException;
}
