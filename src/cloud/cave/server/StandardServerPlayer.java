package cloud.cave.server;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import cloud.cave.common.CaveStorageException;
import cloud.cave.server.service.ServerWeatherService;
import com.google.common.base.Strings;
import org.json.simple.JSONObject;

import cloud.cave.domain.*;
import cloud.cave.ipc.*;
import cloud.cave.server.common.*;
import cloud.cave.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard implementation of Player on the server side. Interacts with
 * underlying storage for mutator methods, and some of the more complex accessor
 * methods.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class StandardServerPlayer implements Player {
    /**
     * The classpath used to search for Command objects
     */
    public static final String EXTENSION_CLASSPATH = "cloud.cave.extension";
    private static final Logger logger = LoggerFactory.getLogger(StandardServerPlayer.class);

    private final CaveStorage storage;
    private final String ID;

    // These attributes of the player are essentially
    // caching of the 'true' information which is stored in
    // the underlying cave storage.
    private String name;
    private String groupName;
    private Region region;
    private RoomRecord currentRoom;
    private String position;
    private String sessionId;

    private WeatherService weatherService;

    /**
     * Never call this constructor directly, use cave.login() instead!
     * Create a player instance bound to the given delegates for
     * session caching, persistence, etc.
     *
     * @param playerID       the player's id
     * @param storage        the storage service connector for the cave and players
     * @param weatherService the weather service connector
     * @param sessionCache   the cache of player sessions to use
     */
    StandardServerPlayer(String playerID, CaveStorage storage, WeatherService weatherService, PlayerSessionCache sessionCache) {
        super();
        this.ID = playerID;
        this.storage = storage;
        this.weatherService = weatherService;

        refreshFromStorage();
    }

    @Override
    public void addMessage(String message) {
        final String formattedMessage = String.format("[%s] %s", this.name, message.trim());
        currentRoom.addMessage(formattedMessage);
    }


    private final ReentrantLock lock = new ReentrantLock();
    @Override
    public boolean move(Direction direction) {
        // Calculate the offsets in the given direction
        final Point3 p = Point3.parseString(position);
        p.translate(direction);
        // convert to the new position in string format
        final String newPosition = p.getPositionString();
        // get the room in that direction
        final RoomRecord newRoom = storage.getRoom(newPosition);
        // if it is null, then there is no room in that direction
        if (newRoom == null) {
            return false;
        }
        // otherwise update internal state
        position = p.getPositionString();
        currentRoom = newRoom;

        try {
            lock.tryLock(5, TimeUnit.SECONDS);

            // and update this player's position in the storage
            final PlayerRecord pRecord = storage.getPlayerByID(this.getID());
            final PlayerRecord newRecord = new PlayerRecord(pRecord.getPlayerID(),
                    pRecord.getPlayerName(),
                    pRecord.getGroupName(),
                    pRecord.getRegion(),
                    position,
                    pRecord.getSessionId());

            try {
                storage.updatePlayerRecord(newRecord);
                return true;
            } catch (CaveStorageException e) {
                throw e;
            } catch (RuntimeException e) {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean digRoom(Direction direction, String description) {
        // Calculate the offsets in the given direction
        final Point3 p = Point3.parseString(position);
        p.translate(direction);
        final RoomRecord room = new RoomRecord(description, new ArrayList<String>());
        return storage.addRoom(p.getPositionString(), room);
    }

    @Override
    public JSONObject execute(String commandName, String... parameters) {
        // Compute the qualified path of the command class that shall be loaded
        final String qualifiedClassName = EXTENSION_CLASSPATH + "." + commandName;

        // Load it
        final Class<?> theClass;
        try {
            theClass = Class.forName(qualifiedClassName);
        } catch (ClassNotFoundException e) {
            return Marshaling.createInvalidReplyWithExplantion(StatusCode.SERVER_FAILED_TO_LOAD_COMMAND,
                    "Player.execute failed to load Command class: " + commandName);
        }

        // Next, instantiate the command object
        final Command command;
        try {
            command = (Command) theClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return Marshaling.createInvalidReplyWithExplantion(StatusCode.SERVER_FAILED_TO_INSTANTIATE_COMMAND,
                    "Player.execute failed to instantiate Command object: " + commandName);
        }

        final JSONObject reply;
        try  {
            lock.tryLock(5, TimeUnit.SECONDS);

            // Initialize the command object
            command.setPlayerID(getID());
            command.setStorageService(storage);

            // And execute the command...
            reply = command.execute(parameters);

            // as the command may update any aspect of the player' data
            // and as we cache it here locally, invalidate the caching
            refreshFromStorage();
        } catch (InterruptedException e) {
            return null;
        } finally {
            lock.unlock();
        }

        return reply;
    }

    //
    // Only getters below here, no mutable
    //

    @Override
    public List<Direction> getExitSet() {
        return storage.getSetOfExitsFromRoom(position);
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public String getPosition() {
        return storage.getPlayerByID(getID()).getPositionAsString();
    }

    @Override
    public List<String> getPlayersHere(int offset) {
        List<PlayerRecord> playerList = storage.computeListOfPlayersAt(getPosition(), offset);
        List<String> playerNameList = new ArrayList<>();
        for (PlayerRecord record : playerList) {
            playerNameList.add(record.getPlayerName());
        }
        return playerNameList;
    }

    @Override
    public String getSessionID() {
        return sessionId;
    }

    @Override
    public List<String> getMessageList() {
        return currentRoom.getMessageList();
    }


    @Override
    public String getName() {
        return name;
    }

    public String getGroupName() {
        return groupName;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public String getShortRoomDescription() {
        return currentRoom.description;
    }

    @Override
    public String getLongRoomDescription(int offset) {
        if (offset == -1) offset = 0;

        String allOfIt = getShortRoomDescription() + "\nThere are exits in directions:\n";
        for (Direction dir : getExitSet()) {
            allOfIt += "  " + dir + " ";
        }
        allOfIt += "\nYou see other players:\n";
        List<String> playerNameList = getPlayersHere(offset);
        int count = 0;
        for (String p : playerNameList) {
            allOfIt += "  [" + count + "] " + p + "\n";
            count++;
        }

        if (offset == 0 && count < 10)
            allOfIt += " *** End of player list *** \n";
        else if (offset > 0 && count < 20)
            allOfIt += " *** End of player list *** \n";

        return allOfIt;
    }

    @Override
    public String getWeather() {
        JSONObject weatherAsJson = weatherService.requestWeather(getGroupName(), getID(), getRegion());

        // Prevent null pointer crashes etc.
        String errorMessage = Strings.nullToEmpty((String) weatherAsJson.get("errorMessage"));
        String authenticated = Strings.nullToEmpty((String) weatherAsJson.get("authenticated"));

        if (errorMessage.equals(ServerWeatherService.ERROR_MESSAGE_OK))
            return convertToFormattedString(weatherAsJson);
        else if (errorMessage.equals(ServerWeatherService.ERROR_MESSAGE_UNAVAILABLE_CLOSED))
            return "*** Sorry - weather information is not available ***";
        else if (errorMessage.equals(ServerWeatherService.ERROR_MESSAGE_UNAVAILABLE_OPEN))
            return "*** Sorry - no weather (open circuit) ***";
        else if (authenticated.equals("false"))
            return "The weather service failed with message: " + errorMessage;
        else {
            logger.error("An unknow error status was received in getWeather. The JSON received was: " +
                    weatherAsJson.toJSONString());
            return "Unrecognized error message recieved";
        }
    }

    /**
     * Convert a JSON object that represents weather in the format of the cave
     * weather service into the string representation defined for the cave UI.
     *
     * @param currentObservation weather information formatted as JSON
     * @return formatted string describing the weather
     */
    private String convertToFormattedString(JSONObject currentObservation) {
        final String temperature = currentObservation.get("temperature").toString();
        final double tempDouble = Double.parseDouble(temperature);

        final String feelslike = currentObservation.get("feelslike").toString();
        final double feelDouble = Double.parseDouble(feelslike);

        final String winddir = currentObservation.get("winddirection").toString();

        final String windspeed = currentObservation.get("windspeed").toString();
        final double windSpDouble = Double.parseDouble(windspeed);

        final String weather = currentObservation.get("weather").toString();

        final String time = currentObservation.get("time").toString();

        return String.format(Locale.US,
                "The weather in %s is %s, temperature %.1fC (feelslike %.1fC). Wind: %.1f m/s, direction %s. " +
                        "This report is dated: %s.",
                getRegion().toString(), weather, tempDouble, feelDouble, windSpDouble, winddir, time);
    }

    private void refreshFromStorage() {
        PlayerRecord pr = storage.getPlayerByID(ID);
        name = pr.getPlayerName();
        groupName = pr.getGroupName();
        position = pr.getPositionAsString();
        region = pr.getRegion();
        sessionId = pr.getSessionId();

        currentRoom = storage.getRoom(position);
    }

    @Override
    public String toString() {
        return "StandardServerPlayer [storage=" + storage + ", name=" + name
                + ", ID=" + ID + ", region=" + region + ", currentRoom=" + currentRoom
                + ", position=" + position + ", sessionId=" + sessionId + "]";
    }
}
