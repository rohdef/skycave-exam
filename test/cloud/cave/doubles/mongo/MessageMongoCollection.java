package cloud.cave.doubles.mongo;

import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Created by rohdef on 10/10/15.
 */
public class MessageMongoCollection extends AbstractMongoCollection {
    @Override
    public long count(Bson bson) {
        return 0;
    }

    @Override
    public FindIterable<Document> find() {
        return null;
    }

    @Override
    public FindIterable<Document> find(Bson bson) {
        return null;
    }
}
