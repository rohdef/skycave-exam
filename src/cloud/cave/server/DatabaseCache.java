package cloud.cave.server;

import cloud.cave.domain.Player;
import cloud.cave.server.service.ServerCaveStorage;
import cloud.cave.service.CaveStorage;
import cloud.cave.service.WeatherService;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.BSON;
import org.bson.Document;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class DatabaseCache implements PlayerSessionCache {
    private static final String CACHE_COLLECTION = "sessionCache";
    private final CaveStorage storage;
    private final WeatherService weatherService;

    private MongoClient client;
    private MongoDatabase database;
    private MongoCollection<Document> cacheCollection;

    public DatabaseCache(CaveStorage storage, WeatherService weatherService) {
        this.storage = storage;
        this.weatherService = weatherService;

        client = new MongoClient();
        database = client.getDatabase(ServerCaveStorage.DB_NAME);

        cacheCollection = database.getCollection(CACHE_COLLECTION);
    }

    @Override
    public Player get(String playerID) {
        Document document = new Document("playerID", playerID);

        if (cacheCollection.count(document) > 0) {
            return new StandardServerPlayer(playerID, storage, weatherService, this);
        }

        return null;
    }

    @Override
    public void add(String playerID, Player player) {
        Document document = new Document("playerID", playerID);
        cacheCollection.insertOne(document);
    }

    @Override
    public void remove(String playerID) {
        Document document = new Document("playerID", playerID);
        cacheCollection.deleteMany(document);
    }
}
