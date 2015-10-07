package cloud.cave.server.service;

import cloud.cave.domain.Direction;
import cloud.cave.domain.Region;
import cloud.cave.server.common.*;
import cloud.cave.service.CaveStorage;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
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

    private MongoClient mongoClient;
    private MongoDatabase database;
    private ServerConfiguration config;

    // http://mongodb.github.io/mongo-java-driver/3.0/driver/getting-started/quick-tour/
    public ServerCaveStorage() {}

    private void createBaseData() {
        List<String> messageList = new ArrayList<>();
        messageList.add("[Mark] First Like");
        messageList.add("[Rohde] OMG det sagde du bare ikk");
        messageList.add("[Mark] Jo, jeg fik First Like");

        RoomRecord entryRoom = new RoomRecord("You are standing at the end of a road before a small brick building.",
                messageList);
        this.addRoom(new Point3(0, 0, 0).getPositionString(), entryRoom);

        //North
        messageList = new ArrayList<>();
        messageList.add("[Little Prince] Why are you drinking?");
        messageList.add("[Tippler] So that I may forget");
        messageList.add("[Little Prince] Forget what?");
        messageList.add("[Tippler] Forget that I am ashamed");
        messageList.add("[Little Prince] Ashamed of what?");
        messageList.add("[Tippler] Ashamed of drinking!");

        this.addRoom(new Point3(0, 1, 0).getPositionString(),
                new RoomRecord("You are in open forest, with a deep valley to one side.", messageList));

        //East
        this.addRoom(new Point3(1, 0, 0).getPositionString(),
                new RoomRecord("You are inside a building, a well house for a large spring.", new ArrayList<String>()));
        //West
        this.addRoom(new Point3(-1, 0, 0).getPositionString(),
                new RoomRecord("You have walked up a hill, still in the forest.", new ArrayList<String>()));
        //Up
        this.addRoom(new Point3(0, 0, 1).getPositionString(),
                new RoomRecord("You are in the top of a tall tree, at the end of a road.", new ArrayList<String>()));
    }

    @Override
    public RoomRecord getRoom(String positionString) {
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        Document room = roomCollection.find(new Document("_id", positionString)).first();
        logger.debug(positionString);
        if(room != null){
            String description = room.getString("description");

            ArrayList<String> messageList = room.get("messageList", (new ArrayList<String>()).getClass());
            System.out.println(messageList.size());
            return new RoomRecord(description, messageList);
        }
        return null;
    }

    @Override
    public boolean addRoom(String positionString, RoomRecord roomRecord) {
        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        Document room = new Document()
                            .append("_id", positionString)
                            .append("description", roomRecord.description)
                            .append("messageList", roomRecord.getMessageList());
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

            if(roomCollection.count(new Document("_id", position)) > 0)
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
    public List<PlayerRecord> computeListOfPlayersAt(String positionString, int offset) {
        int start = 0;
        int limit = 10;

        if (offset > 0) {
            start = 10 + (20*(offset-1));
            limit = 20;
        }

        MongoCollection<Document> playerCollection = database.getCollection(COLLECTION_PLAYERS);
        FindIterable<Document> playersAt = playerCollection.find(new Document("positionAsString", positionString))
                .sort(Sorts.ascending("_id"))
                .skip(start)
                .limit(limit);
        final LinkedList <PlayerRecord> playersAtLocationList = new LinkedList<>();

        playersAt.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
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
        List<ServerAddress> serverAddresses = new ArrayList<>();

        for (int i = 0; i<config.size(); i++) {
            ServerData hostConfig = config.get(i);
            serverAddresses.add(new ServerAddress(hostConfig.getHostName(), hostConfig.getPortNumber()));
        }


        mongoClient = new MongoClient(serverAddresses);
        database = mongoClient.getDatabase(DB_NAME);

        MongoCollection<Document> roomCollection = database.getCollection(COLLECTION_ROOMS);
        if (roomCollection.count() == 0)
            createBaseData();
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
