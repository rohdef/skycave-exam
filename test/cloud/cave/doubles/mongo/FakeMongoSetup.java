package cloud.cave.doubles.mongo;

import cloud.cave.domain.IMongoSetup;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.service.MongoConnectionSetup;
import cloud.cave.server.service.ServerCaveStorage;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

/**
 * Created by rohdef on 10/10/15.
 */
public class FakeMongoSetup implements IMongoSetup {
    private RuntimeException nextException = null;

    @Override
    public MongoCollection<Document> getCollection(String name) {
        if (nextException != null) {
            RuntimeException exception = nextException;
            nextException = null;
            throw exception;
        }

        switch (name) {
            case ServerCaveStorage.COLLECTION_MESSAGES:
                return new MessageMongoCollection();
            case ServerCaveStorage.COLLECTION_PLAYERS:
                return null;
            case ServerCaveStorage.COLLECTION_ROOMS:
                return null;
            default:
                return null;
        }
    }

    public void setNextException(RuntimeException nextException) {
        this.nextException = nextException;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        // Ignore this, ain't needed
    }

    @Override
    public void disconnect() {
        // Ignore this, ain't needed
    }
}
