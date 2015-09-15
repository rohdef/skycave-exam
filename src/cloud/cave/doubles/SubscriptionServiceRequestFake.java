package cloud.cave.doubles;

import cloud.cave.service.IRestRequest;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class SubscriptionServiceRequestFake implements IRestRequest {
    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException {
        String template = "{\"success\":%1$s," +
                "\"subscription\":" +
                "{\"groupName\":\"%4$s\"," +
                "\"dateCreated\":\"2015-08-31 13:06 PM UTC\"," +
                "\"playerName\":\"%3$s\"," +
                "\"loginName\":\"%2$s\"," +
                "\"region\":\"%5$s\"," +
                "\"playerID\":\"%6$s\"}," +
                "\"message\":\"loginName %2$s was authenticated\"}";
        boolean success;

        String[] urlsParts = url.split("&");
        String loginName = urlsParts[0];
        String password = urlsParts[1];
        String playerName, groupName, region, playerId;


        if (loginName.equals("mikkel_aarskort") && password.equals("123")) {
            success = true;
            playerName = "Mikkel";
            groupName = "grp01";
            region = "ARHUS";
            playerId = "user-001";
        } else if (loginName.equals("magnus_aarskort") && password.equals("312")) {
            success = true;
            playerName = "Magnus";
            groupName = "grp01";
            region = "COPENHAGEN";
            playerId = "user-002";
        } else if (loginName.equals("mathilde_aarskort") && password.equals("321")) {
            success = true;
            playerName = "Mathilde";
            groupName = "grp02";
            region = "AALBORG";
            playerId = "user-003";
        } else if (loginName.equals("reserved_aarskort") && password.equals("cloudarch")) {
            success = true;
            playerName = "ReservedCrunchUser";
            groupName = "zzz0";
            region = "ARHUS";
            playerId = "user-reserved";
        } else {
            success = false;
            playerName = "";
            groupName = "";
            region = "";
            playerId = "";
            template = "{\"success\":%1$s,\"message\":\"loginName %2$s was not authenticated\"}";
        }

        return String.format(template, success, loginName, playerName, groupName, region, playerId);
}
}
