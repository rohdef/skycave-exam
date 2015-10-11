package cloud.cave.server;

import cloud.cave.common.LoginRecord;
import cloud.cave.config.CaveServerFactory;
import cloud.cave.domain.*;
import cloud.cave.ipc.CaveIPCException;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.Point3;
import cloud.cave.server.common.SubscriptionRecord;
import cloud.cave.service.CaveStorage;
import cloud.cave.service.SubscriptionService;
import cloud.cave.service.WeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The standard server side implementation of the Cave. Just as the server side
 * player, this implementation communicates directly with the storage layer to
 * achieve it behavior.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class StandardServerCave implements Cave {
    private final CaveStorage storage;
    private final SubscriptionService subscriptionService;
    private final WeatherService weatherService;
    private final PlayerSessionCache sessionCache;

    private final ReentrantLock lock = new ReentrantLock();
    private static final Logger logger = LoggerFactory.getLogger(StandardServerCave.class);

    public StandardServerCave(CaveServerFactory factory) {
        storage = factory.createCaveStorage();
        subscriptionService = factory.createSubscriptionServiceConnector();
        weatherService = factory.createWeatherServiceConnector();
        sessionCache = factory.createPlayerSessionCache(storage, weatherService);
    }

    /**
     * Given a loginName and password (like '201017201','123') contact the
     * subscription storage to validate that the player is registered. If valid,
     * create the player avatar. Return the result of the login
     *
     * @param loginName the loginName which the player uses to identify his/her account in
     *                  the cave
     * @param password  the password associated with the account
     * @return the result of the login
     */
    @Override
    public Login login(final String loginName, final String password) {
        final Login result;

        // Fetch the subscription for the given loginName
        final SubscriptionRecord subscription;

        try {
            subscription = subscriptionService.lookup(loginName, password);
        } catch (CaveIPCException e) {
            String errorMsg = "Lookup failed on subscription service due to IPC exception:" + e.getMessage();
            logger.error(errorMsg, e);
            return new LoginRecord(LoginResult.LOGIN_FAILED_SERVER_ERROR);
        }

        if (subscription == null) {
            return new LoginRecord(LoginResult.LOGIN_FAILED_SERVER_ERROR);
        }
        // Check all the error conditions and 'fail fast' on them...
        switch (subscription.getErrorCode()) {
            case LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN:
                return new LoginRecord(LoginResult.LOGIN_FAILED_UNKNOWN_SUBSCRIPTION);
            case LOGIN_SERVICE_UNAVAILABLE_CLOSED:
            case LOGIN_SERVICE_UNAVAILABLE_OPEN:
                return new LoginRecord(LoginResult.LOGIN_FAILED_SERVER_ERROR);
            case LOGIN_NAME_HAS_VALID_SUBSCRIPTION:
                break;
            default:
                logger.error("An unknown errorCode for login was recieved and thus rejected. The recieved code is: "
                        + subscription.getErrorCode().name());
                return new LoginRecord(LoginResult.LOGIN_FAILED_SERVER_ERROR);
        }

        // Now the subscription is assumed to be a valid player
        final String playerID = subscription.getPlayerID();

        // Create id of session as a random UUID
        final String sessionID = UUID.randomUUID().toString();

        // Enter the player, creating the player's session in the cave
        // (which may overwrite an already ongoing session which is then
        // implicitly invalidated).
        final LoginResult theResult = startPlayerSession(subscription, sessionID);

        final boolean validLogin = LoginResult.isValidLogin(theResult);
        if (!validLogin) {
            return new LoginRecord(theResult);
        }

        // Create player domain object
        final Player player = new StandardServerPlayer(playerID, storage, weatherService, sessionCache);

        // Cache the player session for faster lookups
        sessionCache.add(playerID, player);

        // And finalize the login result
        result = new LoginRecord(player, theResult);

        return result;
    }

    /**
     * Initialize a player session by updating/preparing the storage system
     * and potentially clear the cache of previous sessions.
     *
     * @param subscription the record of the subscription to start a session on
     * @param sessionID    ID of the session assigned to this login
     * @return result of the login which is always a valid login, but
     * may signal a 'second login' that overrules a previous one.
     */
    private LoginResult startPlayerSession(final SubscriptionRecord subscription,
                                           final String sessionID) {
        final LoginResult result;

        // get the record of the player from storage
        try {
            lock.tryLock(5, TimeUnit.SECONDS);

            final PlayerRecord playerRecord = storage.getPlayerByID(subscription.getPlayerID());
            final PlayerRecord playerRecordCreated;

            if (playerRecord == null) {
                // Apparently a newly registered player, so create the record and add it to the cave storage
                final String position = new Point3(0, 0, 0).getPositionString();
                playerRecordCreated = new PlayerRecord(subscription.getPlayerID(),
                        subscription.getPlayerName(),
                        subscription.getGroupName(),
                        subscription.getRegion(),
                        position,
                        sessionID);
                result = LoginResult.LOGIN_SUCCESS;
            } else {
                // Player has been seen before; if he/she has an existing
                // session ("= is in cave") we flag this as a warning,
                // and clear the cache entry
                if (playerRecord.isInCave()) {
                    result = LoginResult.LOGIN_SUCCESS_PLAYER_ALREADY_LOGGED_IN;
                } else {
                    result = LoginResult.LOGIN_SUCCESS;
                }

                playerRecordCreated = new PlayerRecord(playerRecord.getPlayerID(),
                        playerRecord.getPlayerName(),
                        playerRecord.getGroupName(),
                        playerRecord.getRegion(),
                        playerRecord.getPositionAsString(),
                        sessionID);
            }
            storage.updatePlayerRecord(playerRecordCreated);

            return result;
        } catch (InterruptedException e) {
            return LoginResult.LOGIN_FAILED_SERVER_ERROR;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogoutResult logout(final String playerID) {
        try {
            lock.tryLock(5, TimeUnit.SECONDS);

            final PlayerRecord player = storage.getPlayerByID(playerID);

            if (!player.isInCave()) {
                return LogoutResult.PLAYER_NOT_IN_CAVE;
            }

            // reset the session  to indicate the player is no longer around
            PlayerRecord playerRecordCreated = new PlayerRecord(player.getPlayerID(),
                    player.getPlayerName(),
                    player.getGroupName(),
                    player.getRegion(),
                    player.getPositionAsString(),
                    null);

            // and update the record in the storage
            storage.updatePlayerRecord(playerRecordCreated);
            sessionCache.remove(playerID);

            return LogoutResult.SUCCESS;
        } catch (InterruptedException e) {
            return LogoutResult.SERVER_FAILURE;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String describeConfiguration() {
        String cfg = "StandardServerCave configuration:\n";
        cfg += "  CaveStorage: " + storage.getClass().getName() + " / cfg: " + storage.getConfiguration() + "\n";
        cfg += "  SubscriptionService: " + subscriptionService.getClass().getName() + " / cfg: " + subscriptionService.getConfiguration() + "\n";
        cfg += "  WeatherService: " + weatherService.getClass().getName() + " / cfg: " + weatherService.getConfiguration() + "\n";
        return cfg;
    }

    public PlayerSessionCache getCache() {
        return sessionCache;
    }
}
