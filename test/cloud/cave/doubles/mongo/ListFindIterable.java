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
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rohdef on 10/11/15.
 */
public class ListFindIterable implements FindIterable<Document> {
    private List<Document> documentList;

    public ListFindIterable(List<Document> documentList) {
        this.documentList = documentList;
    }

    @Override
    public FindIterable<Document> filter(Bson bson) {
        return this;
    }

    @Override
    public FindIterable<Document> limit(int i) {
        return this;
    }

    @Override
    public FindIterable<Document> skip(int i) {
        return this;
    }

    @Override
    public FindIterable<Document> maxTime(long l, TimeUnit timeUnit) {
        return this;
    }

    @Override
    public FindIterable<Document> modifiers(Bson bson) {
        return this;
    }

    @Override
    public FindIterable<Document> projection(Bson bson) {
        return this;
    }

    @Override
    public FindIterable<Document> sort(Bson bson) {
        return this;
    }

    @Override
    public FindIterable<Document> noCursorTimeout(boolean b) {
        return this;
    }

    @Override
    public FindIterable<Document> oplogReplay(boolean b) {
        return this;
    }

    @Override
    public FindIterable<Document> partial(boolean b) {
        return this;
    }

    @Override
    public FindIterable<Document> cursorType(CursorType cursorType) {
        return this;
    }

    @Override
    public FindIterable<Document> batchSize(int i) {
        return this;
    }

    @Override
    public MongoCursor<Document> iterator() {
        return null;
    }

    @Override
    public Document first() {
        return documentList.get(0);
    }

    @Override
    public <U> MongoIterable<U> map(Function<Document, U> function) {
        return null;
    }

    @Override
    public void forEach(Block<? super Document> block) {
        for (Document document : documentList) {
            block.apply(document);
        }
    }

    @Override
    public <A extends Collection<? super Document>> A into(A objects) {
        return null;
    }
}
