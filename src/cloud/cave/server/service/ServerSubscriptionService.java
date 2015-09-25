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
    private int cacheTimeout;

    public ServerSubscriptionService() {
        super();
        subscriptionMap = new HashMap<>();
        subscriptionServiceState = new ClosedSubscriptionServiceState(this);
        this.secondsDelay = 10;
        this.cacheTimeout = 60*60*1000;
    }

    @Override
    public SubscriptionRecord lookup(String loginName, String password) {
        return subscriptionServiceState.lookup(loginName, password);
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
     * Sets the timeout of the user cache
     * @param cacheTimeout seconds before the timeout
     */
    public void setCacheTimeout(int cacheTimeout) {
        this.cacheTimeout = cacheTimeout*1000;
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

            this.bCryptHash = BCrypt.hashpw(password, salt);
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
                logger.debug("\u001b[1;33mSubscription Service is CLOSED\u001b[0;37m");
                SubscriptionRecord subscriptionRecord = performLookup(loginName, password);
                retryCount = 0;
                return subscriptionRecord;
            } catch (IllegalArgumentException e) {
                logger.warn("\u001b[0;31mMalformed login details detected\u001b[0;37m", e);
                return new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);
            } catch (Exception e) {
                logger.warn("\u001b[0;31mCould not contact the subscription service [closed]\u001b[0;37m", e);
                this.retryCount++;

                if (this.retryCount >= this.threshold)
                    serverSubscriptionService.setSubscriptionServiceState(new OpenSubscriptionServiceState(serverSubscriptionService));

                return new SubscriptionRecord(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED);
            }
        }

        private SubscriptionRecord performLookup(String loginName, String password) {
            if (serverSubscriptionService.config == null) {
                logger.error("The subscription service was called without having been initialized with a config");
                throw new NullPointerException("The subscription service has not been initialized with a ServerConfiguration," +
                        " this value must be null.");
            }
            if (loginName == null || loginName.length() <= 0)
                throw new IllegalArgumentException("The user must be set to a valid String of length >= 1");
            if (password == null || password.length() <= 0)
                throw new IllegalArgumentException("The password must be set to a String of length >= 1");


            String urlTemplate = "http://%1$s:%2$s/api/v1/auth?loginName=%3$s&password=%4$s";
            String url = String.format(urlTemplate,
                    serverSubscriptionService.getConfiguration().get(0).getHostName(),
                    serverSubscriptionService.getConfiguration().get(0).getPortNumber(),
                    loginName,
                    password);
            String subscriptionString;
            try {
                subscriptionString = restRequest.doRequest(url, null);
            } catch (IOException e) {
                logger.warn("Exception when connecting to the subscription service", e);
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
//                String loginNameResult = (String) subscriptionPart.get("loginName");
                String playerName = (String) subscriptionPart.get("playerName");
                String groupName = (String) subscriptionPart.get("groupName");
                String region = ((String) subscriptionPart.get("region")).replace("Aarhus", "Arhus");
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

        public OpenSubscriptionServiceState(ServerSubscriptionService serverSubscriptionService) {
            this.serverSubscriptionService = serverSubscriptionService;
            this.closingTime = new Date();

            this.timeToHalfOpen = this.closingTime.getTime()+(serverSubscriptionService.secondsDelay*1000);
        }

        @Override
        public SubscriptionRecord lookup(String loginName, String password) {
            logger.info("\u001b[1;33mSubscription service is OPEN, bypassing request\u001b[0;37m");

            long currentTime = (new Date()).getTime();
            if (timeToHalfOpen <= currentTime) {
                ISubscriptionServiceState serviceState = new HalfOpenSubscriptionServiceState(serverSubscriptionService);
                serverSubscriptionService.setSubscriptionServiceState(serviceState);
                return serviceState.lookup(loginName, password);
            } else {
                SubscriptionPair pair = serverSubscriptionService.subscriptionMap.get(loginName);

                // Verify that loginName+pwd match a valid subscription
                if (pair == null)
                    return new SubscriptionRecord(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN);
                if (!BCrypt.checkpw(password, pair.getbCryptHash()))
                    return new SubscriptionRecord(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN);

                long lastLoginPlusTimeout = pair.getTimeStamp().getTime()+serverSubscriptionService.cacheTimeout;
                if (lastLoginPlusTimeout <= currentTime) {
                    serverSubscriptionService.subscriptionMap.remove(loginName);
                    return new SubscriptionRecord(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN);
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
            logger.info("\u001B[1;33mSubscription service is HALF-OPEN, trying normal request\u001b[0;37m");
            ClosedSubscriptionServiceState closedSubscriptionServiceState = new ClosedSubscriptionServiceState(serverSubscriptionService);
            SubscriptionRecord subscriptionRecord = closedSubscriptionServiceState.lookup(loginName, password);

            if (subscriptionRecord.getErrorCode() == SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION) {
                logger.info("\u001B[0;31mContact to the subscription service re-established [half-open]\u001B[0;37m");
                serverSubscriptionService.setSubscriptionServiceState(closedSubscriptionServiceState);
                return subscriptionRecord;
            } else {
                logger.warn("\u001B[0;37mCould not contact the subscription service [half-open]\u001B[0;37m");
                OpenSubscriptionServiceState openSubscriptionServiceState = new OpenSubscriptionServiceState(serverSubscriptionService);
                serverSubscriptionService.setSubscriptionServiceState(openSubscriptionServiceState);
                return openSubscriptionServiceState.lookup(loginName, password);
            }
        }
    }
}
