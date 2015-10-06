package cloud.cave.server.service;

import cloud.cave.domain.Direction;
import cloud.cave.domain.Region;
import cloud.cave.doubles.FakeCaveStorage;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.Point3;
import cloud.cave.server.common.RoomRecord;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.CaveStorage;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;


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
        if(room != null){
            String description = room.getString("description");

            ArrayList<String> messageList = room.get("messageList", (new ArrayList<String>()).getClass());
            System.out.println(messageList.size());
            return new RoomRecord(description, messageList);
        }
        throw new NullPointerException("There is no room");
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
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        List<Direction> listOfExits = new ArrayList<>();
        Point3 pZero = Point3.parseString(positionString);
        Point3 p;
        for (Direction d : Direction.values()) {
            p = new Point3(pZero.x(), pZero.y(), pZero.z());
            p.translate(d);
            String position = p.getPositionString();

            if(roomCollection.count(new Document("position", position)) > 0)
                listOfExits.add(d);

        }
        return listOfExits;
    }

    @Override
    public PlayerRecord getPlayerByID(String playerID) {
        MongoCollection<Document> playerCollection = database.getCollection(COLLECTION_PLAYERS);
        Document players = playerCollection.find(new Document("_id", playerID)).first();
        return documentToPlayerRecord(players);
    }


    @Override
    public void updatePlayerRecord(PlayerRecord record) {
        MongoCollection<Document> playerCollection = database.getCollection(COLLECTION_PLAYERS);
        Document player = new Document()
                .append("_id", record.getPlayerID())
                .append("playerName", record.getPlayerName())
                .append("groupName", record.getGroupName())
                .append("region",record.getRegion().toString())
                .append("positionAsString",record.getPositionAsString())
                .append("sessionID",record.getSessionId());
        playerCollection.replaceOne(new Document("_id", record.getPlayerID()), player, new UpdateOptions().upsert(true));
    }

    @Override
    public List<PlayerRecord> computeListOfPlayersAt(String positionString) {
        MongoCollection<Document> playerCollection = database.getCollection(COLLECTION_PLAYERS);
        FindIterable<Document> playersAt = playerCollection.find(new Document("positionAsString", positionString));
        final LinkedList <PlayerRecord> playersAtLocationList = new LinkedList<>();

        playersAt.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                System.out.println(document);
                playersAtLocationList.add(documentToPlayerRecord(document));
            }
        });

        return playersAtLocationList;
    }

    @Override
    public long computeCountOfActivePlayers() {
        MongoCollection<Document> playerCollection = database.getCollection(COLLECTION_PLAYERS);
        long activePlayers = playerCollection.count(ne("sessionID", null));

        return activePlayers;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
    }

    @Override
    public void disconnect() {
        mongoClient.close();
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
    }

    private PlayerRecord documentToPlayerRecord(Document document){
        PlayerRecord p = null;

        if(document != null){
            String playerID = document.getString("_id");
            String playerName = document.getString("playerName");
            String groupName = document.getString("groupName");
            String positionAsString = document.getString("positionAsString");
            Region region = Region.valueOf(document.getString("region"));
            String sessionID = document.getString("sessionID");


            p = new PlayerRecord(playerID, playerName, groupName, region, positionAsString, sessionID);
        }
        return p;
    }

}
