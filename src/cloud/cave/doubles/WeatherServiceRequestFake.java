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
public class WeatherServiceRequestFake implements IRestRequest {
    private String lastUrl = "";

    @Override
    public String doRequest(String url, List<NameValuePair> params) throws IOException{
        this.lastUrl = url;
        //String[] urlParts = url.split('/');
        String weather;

        String[] urlParts = url.split("/");
        switch (urlParts[urlParts.length-1]) {
            case "Arhus":
                weather = "{\"errorMessage\":\"OK\"," +
                        "\"windspeed\":\"4\"," +
                        "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                        "\"authenticated\":\"true\"," +
                        "\"weather\":\"Partly Cloudy\"," +
                        "\"winddirection\":\"NNE\"," +
                        "\"feelslike\":\"24.0\"," +
                        "\"temperature\":\"42.0\"}";
                break;
            case "Aalborg":
                weather = "{\"errorMessage\":\"OK\"," +
                        "\"windspeed\":\"3\"," +
                        "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                        "\"authenticated\":\"true\"," +
                        "\"weather\":\"Partly Cloudy\"," +
                        "\"winddirection\":\"NNE\"," +
                        "\"feelslike\":\"15.0\"," +
                        "\"temperature\":\"15.0\"}";
                break;
            case "Odense":
                weather = "{\"errorMessage\":\"OK\"," +
                        "\"windspeed\":\"15\"," +
                        "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                        "\"authenticated\":\"true\"," +
                        "\"weather\":\"Partly Cloudy\"," +
                        "\"winddirection\":\"NNE\"," +
                        "\"feelslike\":\"21.0\"," +
                        "\"temperature\":\"21.0\"}";
                break;
            case "Copenhagen":
                weather = "{\"errorMessage\":\"OK\"," +
                        "\"windspeed\":\"25\"," +
                        "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                        "\"authenticated\":\"true\"," +
                        "\"weather\":\"Partly Cloudy\"," +
                        "\"winddirection\":\"NNE\"," +
                        "\"feelslike\":\"-52.0\"," +
                        "\"temperature\":\"-14.0\"}";
                break;
            default:
                weather = null;
        }

        if (!urlParts[urlParts.length-3].equals("grp01"))
            return String.format("{\"errorMessage\": \"GroupName %1$s or playerID %2$s is not authenticated.\"," +
                            "\"authenticated\": \"false\" }",
                    urlParts[urlParts.length-3], urlParts[urlParts.length-2]);

        return weather;
    }

    public String getLastUrl() {
        return lastUrl;
    }
}
