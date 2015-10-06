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
    private String lastUrl;

    @Override
    public void setBuggySupport(boolean buggySupport) {
        // Do nothing, not relevant here
    }

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException {
        lastUrl = url;

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

        String[] urlsParts = url.split("[?]")[1].split("&");
        String loginName = urlsParts[0].split("=")[1];
        String password = urlsParts[1].split("=")[1];
        String playerName, groupName, region, playerId;


        if (loginName.equals("mikkel_aarskort") && password.equals("123")) {
            success = true;
            playerName = "Mikkel";
            groupName = "grp01";
            region = "AARHUS";
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
            region = "AARHUS";
            playerId = "user-reserved";
        } else if (loginName.equals("rwar400t") && password.equals("727b9c")) {
            success = true;
            playerName = "rwar400t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar400t";
        } else if (loginName.equals("rwar401t") && password.equals("ynizl2")) {
            success = true;
            playerName = "rwar401t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar401t";
        } else if (loginName.equals("rwar402t") && password.equals("f0s4p3")) {
            success = true;
            playerName = "rwar402t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar402t";
        } else if (loginName.equals("rwar403t") && password.equals("plcs74")) {
            success = true;
            playerName = "rwar403t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar403t";
        } else if (loginName.equals("rwar404t") && password.equals("v76ifd")) {
            success = true;
            playerName = "rwar404t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar404t";
        } else if (loginName.equals("rwar405t") && password.equals("jxe9ha")) {
            success = true;
            playerName = "rwar405t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar405t";
        } else if (loginName.equals("rwar406t") && password.equals("6xp9jl")) {
            success = true;
            playerName = "rwar406t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar406t";
        } else if (loginName.equals("rwar407t") && password.equals("u3mxug")) {
            success = true;
            playerName = "rwar407t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar407t";
        } else if (loginName.equals("rwar408t") && password.equals("trv9gy")) {
            success = true;
            playerName = "rwar408t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar408t";
        } else if (loginName.equals("rwar409t") && password.equals("1d5fh3")) {
            success = true;
            playerName = "rwar409t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar409t";
        } else if (loginName.equals("rwar410t") && password.equals("zsafci")) {
            success = true;
            playerName = "rwar410t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar410t";
        } else if (loginName.equals("rwar411t") && password.equals("v324q6")) {
            success = true;
            playerName = "rwar411t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar411t";
        } else if (loginName.equals("rwar412t") && password.equals("2jdfhz")) {
            success = true;
            playerName = "rwar412t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar412t";
        } else if (loginName.equals("rwar413t") && password.equals("zja3ig")) {
            success = true;
            playerName = "rwar413t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar413t";
        } else if (loginName.equals("rwar414t") && password.equals("04nj10")) {
            success = true;
            playerName = "rwar414t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar414t";
        } else if (loginName.equals("rwar415t") && password.equals("zu5qar")) {
            success = true;
            playerName = "rwar415t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar415t";
        } else if (loginName.equals("rwar416t") && password.equals("qildw2")) {
            success = true;
            playerName = "rwar416t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar416t";
        } else if (loginName.equals("rwar417t") && password.equals("61w8sh")) {
            success = true;
            playerName = "rwar417t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar417t";
        } else if (loginName.equals("rwar418t") && password.equals("exwt5w")) {
            success = true;
            playerName = "rwar418t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar418t";
        } else if (loginName.equals("rwar419t") && password.equals("n7lzqw")) {
            success = true;
            playerName = "rwar419t";
            groupName = "RWA4";
            region = "AALBORG";
            playerId = "rwar419t";
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

    public String getLastUrl() {
        return lastUrl;
    }
}
