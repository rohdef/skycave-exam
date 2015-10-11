package cloud.cave.doubles.mongo;

import com.mongodb.Block;
import com.mongodb.CursorType;
import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rohdef on 10/11/15.
 */
public class RoomMongoCollection extends AbstractMongoCollection {
    private FindIterable<Document> documentFindIterable;

    public RoomMongoCollection() {
        List<Document> documentList = new LinkedList<>();
        Document room = new Document()
                .append("_id", "(0,0,0)")
                .append("description", "First room");
        documentList.add(room);

        room = new Document()
                .append("_id", "(1,0,0)")
                .append("description", "Second room");
        documentList.add(room);

        room = new Document()
                .append("_id", "(0,1,0)")
                .append("description", "Third room");
        documentList.add(room);

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
        if (bson.toString().contains("null")) {
            return new ListFindIterable(new LinkedList<Document>());
        }
        return documentFindIterable;
    }
}
