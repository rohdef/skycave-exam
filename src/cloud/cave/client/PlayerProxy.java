package cloud.cave.client;

import java.util.*;

import org.json.simple.*;

import cloud.cave.common.PlayerSessionExpiredException;
import cloud.cave.domain.*;
import cloud.cave.ipc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Proxy (Flexible, Reliable Software, p. 317) or more specifically a
 * Client Proxy (Patterns-oriented Software Architecture, Vol 4, p. 240) for
 * the Player role.
 * <p/>
 * Never instantiate a player proxy instance directly, instead you must
 * create a cave proxy, and use its login method to retrieve the player
 * proxy.
 * <p/>
 * All methods follow the same remote proxy template
 * <ol>
 * <li> Marshal this and parameters into a request object in JSON
 * <li> Send it to server and await reply
 * <li> Convert reply object back into return values
 * </ol>
 * It may also become a Half-Object Plus Protocol (Patterns-oriented Software
 * Architecture, Vol 4, p. 324).
 * <p/>
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class PlayerProxy implements Player {
    private ClientRequestHandler crh;

    private String playerID;
    private String playerName;
    private String sessionID;

    private JSONObject requestJson;

    private static Logger logger = LoggerFactory.getLogger(PlayerProxy.class);

    /**
     * DO NOT USE THIS CONSTRUCTOR DIRECTLY (except in unit tests perhaps). Create
     * the player proxy configured with the relevant request handler and
     * properties.
     *
     * @param crh        the client request handler to communicate with the server based
     *                   player instance
     * @param playerID   id of the player
     * @param playerName name of the player
     * @param sessionID  id of the session initiated by this player's login
     */
    PlayerProxy(ClientRequestHandler crh,
                String playerID, String playerName, String sessionID) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.sessionID = sessionID;
        this.crh = crh;
    }

    @Override
    public String getID() {
        return playerID;
    }

    @Override
    public String getShortRoomDescription() {
        // Build the request object
        requestJson = createRequestObject(MarshalingKeys.GET_SHORT_ROOM_DESCRIPTION_METHOD_KEY, "");
        // send the request over the connector and retrieve the reply object
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        // and finally, demarshal the returned value
        return replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
    }

    @Override
    public String getLongRoomDescription() {
        requestJson = createRequestObject(MarshalingKeys.GET_LONG_ROOM_DESCRIPTION_METHOD_KEY, "");
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        return replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public String getSessionID() {
        return sessionID;
    }

    @Override
    public Region getRegion() {
        requestJson = createRequestObject(MarshalingKeys.GET_REGION_METHOD_KEY,
                "");
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        return Region.valueOf(asString);
    }

    @Override
    public boolean move(Direction direction) {
        requestJson = createRequestObject(MarshalingKeys.MOVE_METHOD_KEY,
                direction.toString());
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        return Boolean.parseBoolean(asString);
    }

    @Override
    public boolean digRoom(Direction direction, String description) {
        JSONObject requestJson =
                Marshaling.createRequestObject(playerID,
                        sessionID, MarshalingKeys.DIG_ROOM_METHOD_KEY,
                        "" + direction.toString(), description);
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        return Boolean.parseBoolean(asString);
    }

    @Override
    public String getPosition() {
        requestJson = createRequestObject(MarshalingKeys.GET_POSITION_METHOD_KEY, null);
        JSONObject replyJson = requestAndAwaitReply(requestJson);

        return replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
    }

    @Override
    public List<Direction> getExitSet() {
        requestJson = createRequestObject(MarshalingKeys.GET_EXITSET_METHOD_KEY, null);
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        // The HEAD is not used, the list of player names are stored in the TAIL
        List<Direction> exitsHere = new ArrayList<>();
        JSONArray array = (JSONArray) replyJson.get(MarshalingKeys.RETURNVALUE_TAIL_KEY);
        // Convert the string values back into enums
        for (Object item : array) {
            exitsHere.add(Direction.valueOf(item.toString()));
        }
        return exitsHere;
    }

    @Override
    public List<String> getPlayersHere() {
        JSONObject requestJson = createRequestObject(MarshalingKeys.GET_PLAYERS_HERE_METHOD_KEY, "");
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        // The HEAD is not used, the list of player names are stored in the TAIL
        List<String> playersHere = new ArrayList<>();
        JSONArray array = (JSONArray) replyJson.get(MarshalingKeys.RETURNVALUE_TAIL_KEY);
        // Convert it into a string list
        for (Object item : array) {
            playersHere.add(item.toString());
        }
        return playersHere;
    }

    @Override
    public void addMessage(String message) {
        JSONObject requestJson = Marshaling.createRequestObject(playerID,
                sessionID,
                MarshalingKeys.ADD_MESSAGE_METHOD_KEY,
                message);
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        if (!asString.equals(StatusCode.OK)) {
            // TODO possible error handling, return OK expected
            logger.error("Adding a message to the wall failed, the server response was: " + asString);
        }
    }

    @Override
    public List<String> getMessageList() {
        List<String> contents = new ArrayList<>();

        JSONObject requestJson = Marshaling.createRequestObject(playerID,
                sessionID,
                MarshalingKeys.GET_MESSAGE_LIST_METHOD_KEY,
                null);
        JSONObject replyJson = requestAndAwaitReply(requestJson);

        JSONArray array = (JSONArray) replyJson.get(MarshalingKeys.RETURNVALUE_TAIL_KEY);

        for (Object item : array) {
            contents.add(item.toString());
        }

        return contents;
    }

    @Override
    public String getWeather() {
        JSONObject requestJson = Marshaling.createRequestObject(playerID, sessionID, MarshalingKeys.GET_WEATHER_METHOD_KEY, null);
        JSONObject replyJson = requestAndAwaitReply(requestJson);

        return replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
    }

    @Override
    public JSONObject execute(String commandName, String... parameters) {
        JSONObject requestJson =
                Marshaling.createRequestObject(playerID, sessionID, MarshalingKeys.EXECUTE_METHOD_KEY, commandName, parameters);
        return requestAndAwaitReply(requestJson);
    }

    @Override
    public String toString() {
        return "(PlayerClientProxy: " + getID() + "/" + getName() + ")";
    }

    public JSONObject lastSentRequestObject() {
        return requestJson;
    }

    private JSONObject createRequestObject(String methodKey,
                                           String parameter) {
        return Marshaling.createRequestObject(playerID, sessionID, methodKey, parameter);
    }

    private JSONObject requestAndAwaitReply(JSONObject requestJson) {
        JSONObject replyJson = ClientCommon.requestAndAwaitReply(crh, requestJson);
        String statusCode = replyJson.get(MarshalingKeys.ERROR_CODE_KEY).toString();
        if (statusCode.equals(StatusCode.SERVER_PLAYER_SESSION_EXPIRED_FAILURE)) {
            String errMsg = replyJson.get(MarshalingKeys.ERROR_MSG_KEY).toString();
            throw new PlayerSessionExpiredException(errMsg);
        }

        return replyJson;
    }
}
