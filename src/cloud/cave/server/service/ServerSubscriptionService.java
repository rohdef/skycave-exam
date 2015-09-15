package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.common.SubscriptionRecord;
import cloud.cave.server.common.SubscriptionResult;
import cloud.cave.service.IRestRequest;
import cloud.cave.service.SubscriptionService;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerSubscriptionService implements SubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(ServerSubscriptionService.class);
    private ISubscriptionServiceState subscriptionServiceState;
    private int secondsDelay;

    /**
     * A database 'table' that has loginName as primary key (key)
     * and the subscription record as value.
     */
    private Map<String, SubscriptionPair> subscriptionMap;
    private ServerConfiguration config;

    private IRestRequest restRequest;

    public ServerSubscriptionService() {
        super();
        subscriptionMap = new HashMap<>();
        subscriptionServiceState = new ClosedSubscriptionServiceState(this);
        this.secondsDelay = 10;
    }

    @Override
    public SubscriptionRecord lookup(String loginName, String password) {
        return subscriptionServiceState.lookup(loginName, password);
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
    }

    @Override
    public void disconnect() {
        subscriptionMap.clear();
        subscriptionMap = null;
        subscriptionServiceState = null;
        config = null;
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
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
    public void setSecondsDelay(int secondsDelay) {
        if (secondsDelay <= 0)
            throw new IllegalArgumentException("The delay must be a positve integer");

        this.secondsDelay = secondsDelay;
    }

    public void setSubscriptionServiceState(ISubscriptionServiceState subscriptionServiceState) {
        this.subscriptionServiceState = subscriptionServiceState;
    }

    /**
     * Subscription cache class
     */
    private class SubscriptionPair {
        private Date timeStamp;
        private String bCryptHash;
        private SubscriptionRecord subscriptionRecord;

        public SubscriptionPair(String password, SubscriptionRecord record) {
            String salt = BCrypt.gensalt(4); // Preferring faster over security
            String hash = BCrypt.hashpw(password, salt);

            this.bCryptHash = hash;
            this.subscriptionRecord = record;
            this.timeStamp = new Date();
        }

        public Date getTimeStamp() {
            return timeStamp;
        }

        public String getbCryptHash() {
            return bCryptHash;
        }

        public SubscriptionRecord getSubscriptionRecord() {
            return subscriptionRecord;
        }
    }

    /*
     *
     * STATE MACHINE
     *
     */

    private interface ISubscriptionServiceState {
        SubscriptionRecord lookup(String loginName, String password);
    }

    private class ClosedSubscriptionServiceState implements ISubscriptionServiceState {
        private ServerSubscriptionService serverSubscriptionService;
        private int retryCount, threshold;

        public ClosedSubscriptionServiceState(ServerSubscriptionService serverSubscriptionService) {
            this.serverSubscriptionService = serverSubscriptionService;
            this.retryCount = 0;
            this.threshold = 3;
        }

        @Override
        public SubscriptionRecord lookup(String loginName, String password) {
            try {
                SubscriptionRecord subscriptionRecord = performLookup(loginName, password);
                retryCount = 0;
                return subscriptionRecord;
            } catch (Exception e) {
                logger.warn("Could not contact the weather service [open]", e);
                this.retryCount++;

                if (this.retryCount >= this.threshold)
                    serverSubscriptionService.setSubscriptionServiceState(new OpenSubscriptionServiceState(serverSubscriptionService));

                return new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);
            }
        }

        private SubscriptionRecord performLookup(String loginName, String password) {
//            if (serverWeatherService.config == null) {
//                logger.error("The weather service was called without having been initialized with a config");
//                throw new IllegalStateException("The weather service has not been initialized with a ServerConfiguration," +
//                        " this value must be null.");
//            }
//            if (groupName == null || groupName.length() <= 0)
//                throw new IllegalArgumentException("The group must be set to a valid group name");
//            if (playerID == null || playerID.length() <= 0)
//                throw new IllegalArgumentException("The user must be set to a valid playerId");
//            if (region == null)
//                throw new NullPointerException("The region must be set");

            String url = "cavereg.baerbak.com:4567/api/v1/auth?loginName=%1$s&password=%2$s";
            String subscriptionString;
            try {
                subscriptionString = restRequest.doRequest(loginName + "&" + password, null);
            } catch (IOException e) {
                throw new RuntimeException("An error occured in the connection to the subscription REST service.", e);
            } catch (Exception e) {
                logger.error("Fatal error in the subscription service while requesting the service", e);
                throw new RuntimeException("An error occured in the connection to the subscription REST service.", e);
            }

            JSONObject subscriptionJson;
            JSONParser parser = new JSONParser();
            try {
                subscriptionJson = (JSONObject) parser.parse(subscriptionString);
            } catch (ParseException e) {
                throw new RuntimeException("Invalid JSON returned from the service", e);
            } catch (Exception e) {
                logger.error("Fatal error in the weather service while parsin the JSON", e);
                throw new RuntimeException("Invalid JSON returned from the service", e);
            }

            SubscriptionRecord subscriptionRecord;
            if (subscriptionJson.containsKey("success") && subscriptionJson.get("success").equals(true)) {
                JSONObject subscriptionPart = (JSONObject) subscriptionJson.get("subscription");
                String loginNameResult = (String) subscriptionPart.get("loginName");
                String playerName = (String) subscriptionPart.get("playerName");
                String groupName = (String) subscriptionPart.get("groupName");
                String region = (String) subscriptionPart.get("region");
                String playerId = (String) subscriptionPart.get("playerID");

                subscriptionRecord = new SubscriptionRecord(playerId, playerName,
                        groupName, Region.valueOf(region));

                subscriptionMap.put(loginName, new SubscriptionPair(password, subscriptionRecord));
            } else {
                subscriptionRecord = new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);
            }

            return subscriptionRecord;
        }
    }

    private class OpenSubscriptionServiceState implements ISubscriptionServiceState {
        private final long timeToHalfOpen;
        private ServerSubscriptionService serverSubscriptionService;
        private Date closingTime;
        // 60 minutes * 60 seconds * 1000 milliseconds = 1 hour
        private final long cacheTimeout = 60*60*1000;

        public OpenSubscriptionServiceState(ServerSubscriptionService serverSubscriptionService) {
            this.serverSubscriptionService = serverSubscriptionService;
            this.closingTime = new Date();

            this.timeToHalfOpen = this.closingTime.getTime()+(serverSubscriptionService.secondsDelay*1000);
        }

        @Override
        public SubscriptionRecord lookup(String loginName, String password) {
            logger.info("Subscription service is open, bypassing request");

            long currentTime = (new Date()).getTime();
            if (timeToHalfOpen <= currentTime) {
                ISubscriptionServiceState serviceState = new HalfOpenSubscriptionServiceState(serverSubscriptionService);
                serverSubscriptionService.setSubscriptionServiceState(serviceState);
                return serviceState.lookup(loginName, password);
            } else {
                SubscriptionPair pair = serverSubscriptionService.subscriptionMap.get(loginName);

                // Verify that loginName+pwd match a valid subscription
                if (pair == null || !BCrypt.checkpw(password, pair.bCryptHash)) {
                    return new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);
                }

                long lastLoginPlusTimeout = pair.getTimeStamp().getTime()+cacheTimeout;
                if (lastLoginPlusTimeout <= currentTime) {
                    serverSubscriptionService.subscriptionMap.remove(loginName);
                    return new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);
                }

                return pair.getSubscriptionRecord();
            }
        }
    }

    private class HalfOpenSubscriptionServiceState implements ISubscriptionServiceState {
        private ServerSubscriptionService serverSubscriptionService;

        public HalfOpenSubscriptionServiceState(ServerSubscriptionService serverSubscriptionService) {
            this.serverSubscriptionService = serverSubscriptionService;
        }

        @Override
        public SubscriptionRecord lookup(String loginName, String password) {
//            ClosedWeatherService closedWeatherService = new ClosedWeatherService(serverWeatherService);
//            JSONObject jsonObject = closedWeatherService.requestWeather(groupName, playerID, region);
//
//            if (jsonObject.containsKey("errorMessage") && jsonObject.get("errorMessage").equals(ERROR_MESSAGE_OK)) {
//                logger.info("Contact to the weather service re-established [half-open]");
//                serverWeatherService.setWeatherServiceState(closedWeatherService);
//                return jsonObject;
//            } else {
//                logger.warn("Could not contact the weather service [half-open]");
//                OpenWeatherService openWeatherService = new OpenWeatherService(serverWeatherService);
//                serverWeatherService.setWeatherServiceState(openWeatherService);
//                return openWeatherService.requestWeather(groupName, playerID, region);
//            }
//        }
            return null;
        }
    }
}
