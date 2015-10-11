package cloud.cave.doubles.mongo;

import cloud.cave.domain.Region;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by rohdef on 10/10/15.
 */
public class MessageMongoCollection extends AbstractMongoCollection {
    private FindIterable<Document> documentFindIterable;

    public MessageMongoCollection() {
        List<Document> documentList = new LinkedList<>();
        Document message = new Document()
                .append("room", "(0,0,0)")
                .append("message", "Hello there")
                .append("timestamp", new Date().getTime());
        documentList.add(message);

        message = new Document()
                .append("room", "(0,1,0)")
                .append("message", "fantastic")
                .append("timestamp", new Date().getTime());
        documentList.add(message);

        message = new Document()
                .append("room", "(0,0,0)")
                .append("message", "weather")
                .append("timestamp", new Date().getTime());
        documentList.add(message);

        documentFindIterable = new ListFindIterable(documentList);
    }

    @Override
    public long count(Bson bson) {
        return 3;
    }

    @Override
    public FindIterable<Document> find() {
        return documentFindIterable;
    }

    @Override
    public FindIterable<Document> find(Bson bson) {
        return documentFindIterable;
    }
}
