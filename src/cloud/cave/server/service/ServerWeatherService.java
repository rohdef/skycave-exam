package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.IRestRequest;
import cloud.cave.service.WeatherService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerWeatherService implements WeatherService {
    private IRestRequest restRequest;
    private ServerConfiguration config;

    public ServerWeatherService() {}

    public ServerWeatherService(IRestRequest restRequest) {
        this.restRequest = restRequest;
    }

    @Override
    public JSONObject requestWeather(String groupName, String playerID, Region region) {
        if (this.config == null)
            throw new IllegalStateException("The weather service has not been initialized with a ServerConfiguration," +
                    " this value must be null.");
        if (groupName == null || groupName.length() <= 0)
            throw new IllegalArgumentException("The group must be set to a valid group name");
        if (playerID == null || playerID.length() <= 0)
            throw new IllegalArgumentException("The user must be set to a valid playerId");
        if (region == null)
            throw new NullPointerException("The region must be set");
        if (restRequest == null)
            throw new NullPointerException("The rest request must be set either through the constructer or the setRestRequster method.");

        String regionString = region.name().substring(0, 1).toUpperCase() + region.name().substring(1).toLowerCase();
        String url = String.format("http://%s:%s/cave/weather/api/v1/%s/%s/%s",
                config.get(0).getHostName(),
                config.get(0).getPortNumber(),
                groupName,
                playerID,
                regionString);

        String weatherString = null;
        try {
            weatherString = restRequest.doRequest(url, null);
        } catch (IOException e) {
            throw new RuntimeException("An error occured in the connection to the weather REST service.", e);
        }

        JSONObject weatherJson;
        JSONParser parser = new JSONParser();
        try {
            weatherJson = (JSONObject) parser.parse(weatherString);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JSON returned from the service", e);
        }

        return weatherJson;
    }

    @Override
    public void setRestRequester(IRestRequest restRequest) {
        this.restRequest = restRequest;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        if (config == null)
            throw new NullPointerException("The ServerConfiguration must be set");
        if (config.get(0).getHostName() == null || config.get(0).getHostName().length() <= 0)
            throw new IllegalArgumentException("The host must be set to a sensible value that is a string containing a host");
        if (config.get(0).getPortNumber() <= 0)
            throw new IllegalArgumentException("The port must be set to a sensible value that is a positive integer");

        this.config = config;
    }

    @Override
    public void disconnect() {
        restRequest = null;
        config = null;
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
    }
}