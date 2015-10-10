package cloud.cave.domain;

import cloud.cave.server.common.ServerConfiguration;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

/**
 * Created by rohdef on 10/10/15.
 */
public interface IMongoSetup {
    MongoCollection<Document> getCollection(String name);
    void initialize(ServerConfiguration config);
    void disconnect();
}
