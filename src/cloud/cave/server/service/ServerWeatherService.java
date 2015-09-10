package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.IRestRequest;
import cloud.cave.service.WeatherService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerWeatherService implements WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(ServerWeatherService.class);

    private IRestRequest restRequest;
    private ServerConfiguration config;
    private int retryCount, threshold;
    private int secondsDelay;
    private Date closingTime;

    public static final String ERROR_MESSAGE_OK = "OK";
    public static final String ERROR_MESSAGE_UNAVAILABLE_CLOSED = "UNAVAILABLE-CLOSED";
    public static final String ERROR_MESSAGE_UNAVAILABLE_OPEN = "UNAVAILABLE-OPEN";

    public ServerWeatherService() {}

    public ServerWeatherService(IRestRequest restRequest) {
        if (restRequest == null) {
            throw new NullPointerException("The rest request must be set");
        }

        this.restRequest = restRequest;
        this.retryCount = 0;
        this.threshold = 3;
        this.secondsDelay = 10;
    }

    @Override
    public JSONObject requestWeather(String groupName, String playerID, Region region) {
        if (this.retryCount >= this.threshold) {
            long timeToHalfOpen = this.closingTime.getTime()+(this.secondsDelay*1000);
            long currentTime = (new Date()).getTime();
            if (timeToHalfOpen <= currentTime) {
                try {
                    JSONObject jsonObject = requestWeatherClosed(groupName, playerID, region);

                    return jsonObject;
                } catch (Exception e) {
                    logger.warn("Could not contact the weather service [half-open]", e);

                    this.closingTime = new Date();
                    return requestWeatherOpen(groupName, playerID, region);
                }
            } else {
                logger.info("Weather service is open, bypassing request");
                return requestWeatherOpen(groupName, playerID, region);
            }
        } else {
            try {
                return requestWeatherClosed(groupName, playerID, region);
            } catch (Exception e) {
                logger.warn("Could not contact the weather service [open]", e);
                this.retryCount++;

                if (this.retryCount >= this.threshold) this.closingTime = new Date();

                JSONObject unavailableJson = new JSONObject();
                unavailableJson.put("errorMessage", this.ERROR_MESSAGE_UNAVAILABLE_CLOSED);
                return unavailableJson;
            }
        }
    }

    private JSONObject requestWeatherOpen(String groupName, String playerID, Region region) {
        JSONObject unavailableJson = new JSONObject();
        unavailableJson.put("errorMessage", this.ERROR_MESSAGE_UNAVAILABLE_OPEN);
        return unavailableJson;
    }

    private JSONObject requestWeatherClosed(String groupName, String playerID, Region region) {
        if (this.config == null) {
            logger.error("The weather service was called without having been initialized with a config");
            throw new IllegalStateException("The weather service has not been initialized with a ServerConfiguration," +
                    " this value must be null.");
        }
        if (groupName == null || groupName.length() <= 0)
            throw new IllegalArgumentException("The group must be set to a valid group name");
        if (playerID == null || playerID.length() <= 0)
            throw new IllegalArgumentException("The user must be set to a valid playerId");
        if (region == null)
            throw new NullPointerException("The region must be set");

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
        } catch (Exception e) {
            logger.error("Fatal error in the weather service while requesting the service", e);
            throw new RuntimeException("An error occured in the connection to the weather REST service.", e);
        }

        JSONObject weatherJson;
        JSONParser parser = new JSONParser();
        try {
            weatherJson = (JSONObject) parser.parse(weatherString);
        } catch (ParseException e) {
            throw new RuntimeException("Invalid JSON returned from the service", e);
        } catch (Exception e) {
            logger.error("Fatal error in the weather service while parsin the JSON", e);
            throw new RuntimeException("Invalid JSON returned from the service", e);
        }

        return weatherJson;
    }

    @Override
    public void setRestRequester(IRestRequest restRequest) {
        if (restRequest == null) {
            throw new NullPointerException("The rest request must be set");
        }

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