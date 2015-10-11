package cloud.cave.server.service;

import cloud.cave.domain.IMongoSetup;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.common.ServerData;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rohdef on 10/10/15.
 */
public class MongoConnectionSetup implements IMongoSetup {
    public static final String DB_NAME = "whats-it-going-to-be-then";
    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }

    @Override
    public void initialize(ServerConfiguration config) {
        List<ServerAddress> serverAddresses = new ArrayList<>();

        for (int i = 0; i<config.size(); i++) {
            ServerData hostConfig = config.get(i);
            serverAddresses.add(new ServerAddress(hostConfig.getHostName(), hostConfig.getPortNumber()));
        }


        mongoClient = new MongoClient(serverAddresses);
        database = mongoClient.getDatabase(DB_NAME);
    }

    @Override
    public void disconnect() {
        mongoClient.close();
    }
}
