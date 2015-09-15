package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.IRestRequest;
import cloud.cave.service.WeatherService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.omg.SendingContext.RunTime;
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
    private int secondsDelay;

    private IWeatherServiceState weatherServiceState;

    public static final String ERROR_MESSAGE_OK = "OK";
    public static final String ERROR_MESSAGE_UNAVAILABLE_CLOSED = "UNAVAILABLE-CLOSED";
    public static final String ERROR_MESSAGE_UNAVAILABLE_OPEN = "UNAVAILABLE-OPEN";

    public ServerWeatherService() {
        weatherServiceState = new ClosedWeatherService(this);
        this.secondsDelay = 30;
    }

    public ServerWeatherService(IRestRequest restRequest) {
        this();
        if (restRequest == null) {
            throw new NullPointerException("The rest request must be set");
        }

        this.restRequest = restRequest;
    }

    @Override
    public JSONObject requestWeather(String groupName, String playerID, Region region) {
        return weatherServiceState.requestWeather(groupName, playerID, region);
    }

    @Override
    public void setRestRequester(IRestRequest restRequest) {
        if (restRequest == null) {
            throw new NullPointerException("The rest request must be set");
        }

        this.restRequest = restRequest;
    }

    @Override
    public IRestRequest getRestRequester() {
        return this.restRequest;
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
    public void setSecondsDelay(int secondsDelay) {
        if (secondsDelay <= 0)
            throw new IllegalArgumentException("The delay must be a positve integer");

        this.secondsDelay = secondsDelay;
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
    }

    public void setWeatherServiceState(IWeatherServiceState weatherServiceState) {
        this.weatherServiceState = weatherServiceState;
    }

    /*
     *
     * STATE MACHINE
     *
     */


    private interface IWeatherServiceState {
        JSONObject requestWeather(String groupName, String playerID, Region region);
    }

    private class ClosedWeatherService implements IWeatherServiceState {
        private ServerWeatherService serverWeatherService;
        private int retryCount, threshold;

        public ClosedWeatherService(ServerWeatherService serverWeatherService) {
            this.serverWeatherService = serverWeatherService;
            this.retryCount = 0;
            this.threshold = 3;
        }

        @Override
        public JSONObject requestWeather(String groupName, String playerID, Region region) {
            try {
                JSONObject jsonObject = performRequestWeather(groupName, playerID, region);
                retryCount = 0;
                return jsonObject;
            } catch (Exception e) {
                logger.warn("Could not contact the weather service [open]", e);
                this.retryCount++;

                if (this.retryCount >= this.threshold)
                    serverWeatherService.setWeatherServiceState(new OpenWeatherService(serverWeatherService));

                JSONObject unavailableJson = new JSONObject();
                unavailableJson.put("errorMessage", serverWeatherService.ERROR_MESSAGE_UNAVAILABLE_CLOSED);
                return unavailableJson;
            }
        }

        private JSONObject performRequestWeather(String groupName, String playerID, Region region) {
            if (serverWeatherService.config == null) {
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
                    serverWeatherService.config.get(0).getHostName(),
                    serverWeatherService.config.get(0).getPortNumber(),
                    groupName,
                    playerID,
                    regionString);

            String weatherString;
            try {
                weatherString = serverWeatherService.restRequest.doRequest(url, null);
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

            if (!weatherJson.containsKey("errorMessage"))
                throw new RuntimeException("Invalid json returned, expected errorMessage to be present, but got: " + weatherString);

            return weatherJson;
        }
    }

    private class OpenWeatherService implements IWeatherServiceState {
        private final long timeToHalfOpen;
        private ServerWeatherService serverWeatherService;
        private Date closingTime;

        public OpenWeatherService(ServerWeatherService serverWeatherService) {
            this.serverWeatherService = serverWeatherService;
            this.closingTime = new Date();

            this.timeToHalfOpen = this.closingTime.getTime()+(serverWeatherService.secondsDelay*1000);
        }

        @Override
        public JSONObject requestWeather(String groupName, String playerID, Region region) {
            logger.info("Weather service is open, bypassing request");

            long currentTime = (new Date()).getTime();
            if (timeToHalfOpen <= currentTime) {
                IWeatherServiceState serviceState = new HalfOpenWeatherService(serverWeatherService);
                serverWeatherService.setWeatherServiceState(serviceState);
                return serviceState.requestWeather(groupName, playerID, region);

            } else {
                JSONObject unavailableJson = new JSONObject();
                unavailableJson.put("errorMessage", serverWeatherService.ERROR_MESSAGE_UNAVAILABLE_OPEN);
                return unavailableJson;
            }
        }
    }

    private class HalfOpenWeatherService implements IWeatherServiceState {
        private ServerWeatherService serverWeatherService;

        public HalfOpenWeatherService(ServerWeatherService serverWeatherService) {
            this.serverWeatherService = serverWeatherService;
        }

        @Override
        public JSONObject requestWeather(String groupName, String playerID, Region region) {
            ClosedWeatherService closedWeatherService = new ClosedWeatherService(serverWeatherService);
            JSONObject jsonObject = closedWeatherService.requestWeather(groupName, playerID, region);

            if (jsonObject.containsKey("errorMessage") && jsonObject.get("errorMessage").equals(ERROR_MESSAGE_OK)) {
                logger.info("Contact to the weather service re-established [half-open]");
                serverWeatherService.setWeatherServiceState(closedWeatherService);
                return jsonObject;
            } else {
                logger.warn("Could not contact the weather service [half-open]");
                OpenWeatherService openWeatherService = new OpenWeatherService(serverWeatherService);
                serverWeatherService.setWeatherServiceState(openWeatherService);
                return openWeatherService.requestWeather(groupName, playerID, region);
            }
        }
    }
}