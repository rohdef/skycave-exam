package cloud.cave.doubles.mongo;

import cloud.cave.domain.Region;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedList;
import java.util.List;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class PlayerMongoCollection extends AbstractMongoCollection {
    private FindIterable<Document> documentFindIterable;

    public PlayerMongoCollection() {
        List<Document> documentList = new LinkedList<>();
        Document player = new Document()
                .append("_id", "jabbeerwocky")
                .append("playerName", "Jabberwocky")
                .append("groupName", "evil")
                .append("region", Region.COPENHAGEN.toString())
                .append("positionAsString", "Wonderland")
                .append("sessionID", "318");
        documentList.add(player);

        player = new Document()
                .append("_id", "queen")
                .append("playerName", "Queen Of Hearts")
                .append("groupName", "evil")
                .append("region", Region.COPENHAGEN.toString())
                .append("positionAsString", "Croquet court")
                .append("sessionID", "12");
        documentList.add(player);

        player = new Document()
                .append("_id", "alice")
                .append("playerName", "Alice")
                .append("groupName", "curious")
                .append("region", Region.AARHUS.toString())
                .append("positionAsString", "Wonderland")
                .append("sessionID", "42");
        documentList.add(player);

        documentFindIterable = new ListFindIterable(documentList);
    }

    @Override
    public long count(Bson filter) {
        return 3;
    }

    @Override
    public FindIterable<Document> find() {
        return documentFindIterable;
    }

    @Override
    public FindIterable<Document> find(Bson filter) {
        return documentFindIterable;
    }
}
