package cloud.cave.server.service;

import cloud.cave.domain.Direction;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.RoomRecord;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.CaveStorage;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerCaveStorage implements CaveStorage {
    private static final String DB_NAME = "whats-it-going-to-be-then";
    private static final String COLLECTION_PLAYERS = "droogs";
    private static final String COLLECTION_ROOMS = "rooms";

    private final MongoClient mongoClient;
    private MongoDatabase database;

    // http://mongodb.github.io/mongo-java-driver/3.0/driver/getting-started/quick-tour/
    public ServerCaveStorage() {
        mongoClient = new MongoClient("127.0.0.1", 27017);
        database = mongoClient.getDatabase(DB_NAME);
    }

    @Override
    public RoomRecord getRoom(String positionString) {
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        FindIterable<Document> foo = roomCollection.find();
        return null;
    }

    @Override
    public boolean addRoom(String positionString, RoomRecord description) {
        return false;
    }

    @Override
    public List<Direction> getSetOfExitsFromRoom(String positionString) {
        return null;
    }

    @Override
    public PlayerRecord getPlayerByID(String playerID) {
        return null;
    }

    @Override
    public void updatePlayerRecord(PlayerRecord record) {

    }

    @Override
    public List<PlayerRecord> computeListOfPlayersAt(String positionString) {
        return null;
    }

    @Override
    public int computeCountOfActivePlayers() {
        return 0;
    }

    @Override
    public void initialize(ServerConfiguration config) {

    }

    @Override
    public void disconnect() {
        mongoClient.close();
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return null;
    }
}
