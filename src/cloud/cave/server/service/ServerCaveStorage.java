package cloud.cave.server.service;

import cloud.cave.domain.Direction;
import cloud.cave.doubles.FakeCaveStorage;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.RoomRecord;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.CaveStorage;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerCaveStorage implements CaveStorage {
    public static final String DB_NAME = "whats-it-going-to-be-then";
    private static final String COLLECTION_PLAYERS = "droogs";
    private static final String COLLECTION_ROOMS = "rooms";
    private static final Logger logger = LoggerFactory.getLogger(ServerCaveStorage.class);

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private ServerConfiguration config;
    private FakeCaveStorage fakeCaveStorage = new FakeCaveStorage();

    // http://mongodb.github.io/mongo-java-driver/3.0/driver/getting-started/quick-tour/
    public ServerCaveStorage() {
        mongoClient = new MongoClient();
        database = mongoClient.getDatabase(DB_NAME);
    }

    @Override
    public RoomRecord getRoom(String positionString) {
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        Document room = roomCollection.find(new Document("position", positionString)).first();
        logger.debug(positionString);
        String description = room.getString("description");
        //String[] messages = room.get("messageList", String[].class);

        return new RoomRecord(description, new ArrayList());
    }

    @Override
    public boolean addRoom(String positionString, RoomRecord description) {
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        Document room = new Document()
                            .append("position", positionString)
                            .append("description", description.description)
                            .append("messageList",new ArrayList<String>());
        roomCollection.insertOne(room);
        return true;
    }

    @Override
    public List<Direction> getSetOfExitsFromRoom(String positionString) {
        return fakeCaveStorage.getSetOfExitsFromRoom(positionString);
    }

    @Override
    public PlayerRecord getPlayerByID(String playerID) {
        return fakeCaveStorage.getPlayerByID(playerID);
    }

    @Override
    public void updatePlayerRecord(PlayerRecord record) {
        fakeCaveStorage.updatePlayerRecord(record);
    }

    @Override
    public List<PlayerRecord> computeListOfPlayersAt(String positionString) {
        return fakeCaveStorage.computeListOfPlayersAt(positionString);
    }

    @Override
    public int computeCountOfActivePlayers() {
        return fakeCaveStorage.computeCountOfActivePlayers();
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
        fakeCaveStorage.initialize(config);
    }

    @Override
    public void disconnect() {
        mongoClient.close();
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
    }
}
