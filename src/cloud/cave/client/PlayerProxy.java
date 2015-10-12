package cloud.cave.client;

import java.util.*;

import cloud.cave.common.CaveStorageException;
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
    private final ClientRequestHandler crh;

    private final String playerID;
    private final String playerName;
    private final String sessionID;
    private final Region region;
    private int countCallsForLongRoomDescription;

    private JSONObject requestJson;

    private static final Logger logger = LoggerFactory.getLogger(PlayerProxy.class);
    private String shortRoomDescription;
    private String position;

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
    PlayerProxy(ClientRequestHandler crh, String playerID, String playerName, String sessionID,
                String shortRoomDescription, Region region, String position) {
        this.playerID = playerID;
        this.playerName = playerName;
        this.sessionID = sessionID;
        this.crh = crh;
        this.countCallsForLongRoomDescription = 0;
        this.shortRoomDescription = shortRoomDescription;
        this.region = region;
        this.position = position;
    }

    @Override
    public String getID() {
        return playerID;
    }

    @Override
    public String getShortRoomDescription() {
        this.countCallsForLongRoomDescription = 0;
        return this.shortRoomDescription;
    }

    @Override
    public String getLongRoomDescription(int offset) {
        if (offset == -1) {
            offset = this.countCallsForLongRoomDescription++;
        } else {
            this.countCallsForLongRoomDescription = offset;
        }

        requestJson = createRequestObject(MarshalingKeys.GET_LONG_ROOM_DESCRIPTION_METHOD_KEY, ""+offset);
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String reply = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        String[] lines = reply.split("\n");
        String lastLine = lines[lines.length-1];
        if (lastLine.equals(" *** End of player list *** "))
            this.countCallsForLongRoomDescription = 0;

        return reply;
    }

    @Override
    public String getName() {
        this.countCallsForLongRoomDescription = 0;
        return playerName;
    }

    @Override
    public String getSessionID() {
        this.countCallsForLongRoomDescription = 0;
        return sessionID;
    }

    @Override
    public Region getRegion() {
        this.countCallsForLongRoomDescription = 0;

        return this.region;
    }

    @Override
    public boolean move(Direction direction) {
        this.countCallsForLongRoomDescription = 0;
        requestJson = createRequestObject(MarshalingKeys.MOVE_METHOD_KEY,
                direction.toString());
        final JSONObject replyJson = requestAndAwaitReply(requestJson);
        final String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        this.shortRoomDescription = (String) replyJson.get("shortRoomDescription");
        this.position = (String) replyJson.get("position");
        return Boolean.parseBoolean(asString);
    }

    @Override
    public boolean digRoom(Direction direction, String description) {
        this.countCallsForLongRoomDescription = 0;
        JSONObject requestJson =
                Marshaling.createRequestObject(playerID,
                        sessionID,
                        MarshalingKeys.DIG_ROOM_METHOD_KEY,
                        direction.toString(),
                        description);
        JSONObject replyJson = requestAndAwaitReply(requestJson);
        String asString = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
        return Boolean.parseBoolean(asString);
    }

    @Override
    public String getPosition() {
        this.countCallsForLongRoomDescription = 0;
        return this.position;
    }

    @Override
    public List<Direction> getExitSet() {
        this.countCallsForLongRoomDescription = 0;
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
    public List<String> getPlayersHere(int offset) {
        this.countCallsForLongRoomDescription = 0;
        JSONObject requestJson = createRequestObject(MarshalingKeys.GET_PLAYERS_HERE_METHOD_KEY, "" + offset);
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
        this.countCallsForLongRoomDescription = 0;
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
        this.countCallsForLongRoomDescription = 0;
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

    private String weather;
    private long weatherTimeout = 1000*60*30;
    private long nextWeatherReadingTime = 0;
    @Override
    public String getWeather() {
        this.countCallsForLongRoomDescription = 0;
        if (nextWeatherReadingTime < new Date().getTime()) {
            final JSONObject requestJson = Marshaling.createRequestObject(playerID, sessionID,
                    MarshalingKeys.GET_WEATHER_METHOD_KEY, null);
            final JSONObject replyJson = requestAndAwaitReply(requestJson);
            weather = replyJson.get(MarshalingKeys.RETURNVALUE_HEAD_KEY).toString();
            nextWeatherReadingTime = new Date().getTime()+weatherTimeout;
        }
        return weather;
    }

    public void setWeatherTimeout(long timeout) {
        this.weatherTimeout = timeout;
    }

    @Override
    public JSONObject execute(String commandName, String... parameters) {
        this.countCallsForLongRoomDescription = 0;
        final JSONObject requestJson = Marshaling.createRequestObject(playerID, sessionID,
                MarshalingKeys.EXECUTE_METHOD_KEY, commandName, parameters);
        final JSONObject replyJson = requestAndAwaitReply(requestJson);
        this.shortRoomDescription = (String) replyJson.get("shortRoomDescription");
        replyJson.remove("shortRoomDescription");
        this.position = (String) replyJson.get("position");
        replyJson.remove("position");
        return replyJson;
    }

    @Override
    public String toString() {
        this.countCallsForLongRoomDescription = 0;
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
        } else if (statusCode.equals(StatusCode.SERVER_STORAGE_UNAVAILABLE)) {
            String errMsg = replyJson.get(MarshalingKeys.ERROR_MSG_KEY).toString();
            throw new CaveStorageException(errMsg);
        }

        return replyJson;
    }
}
