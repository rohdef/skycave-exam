package cloud.cave.server;

import cloud.cave.domain.Player;
import cloud.cave.server.service.ServerCaveStorage;
import cloud.cave.service.CaveStorage;
import cloud.cave.service.WeatherService;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class DatabaseCache implements PlayerSessionCache {
    private static final String CACHE_COLLECTION = "sessionCache";
    private final CaveStorage storage;
    private final WeatherService weatherService;

    private MongoCollection<Document> cacheCollection;
    private MongoDatabase database;

    public DatabaseCache(CaveStorage storage, WeatherService weatherService) {
        this.storage = storage;
        this.weatherService = weatherService;

        MongoClient client = new MongoClient(storage.getConfiguration().get(0).getHostName(),
                storage.getConfiguration().get(0).getPortNumber());
        database = client.getDatabase(ServerCaveStorage.DB_NAME);
    }

    @Override
    public Player get(String playerID) {
        cacheCollection = database.getCollection(CACHE_COLLECTION);
        Document document = new Document().append("playerID", playerID);

        if (cacheCollection.count(document) > 0) {
            Document doc = cacheCollection.find(document).first();
            StandardServerPlayer player = new StandardServerPlayer(playerID, storage, weatherService, this);

            return player;
        }

        return null;
    }

    @Override
    public void add(String playerID, Player player) {
        cacheCollection = database.getCollection(CACHE_COLLECTION);
        Document document = new Document().append("playerID", playerID)
                .append("sess", player.getSessionID());
        cacheCollection.insertOne(document);
    }

    @Override
    public void remove(String playerID) {
        cacheCollection = database.getCollection(CACHE_COLLECTION);
        Document document = new Document().append("playerID", playerID);
        cacheCollection.deleteMany(document);
    }
}
