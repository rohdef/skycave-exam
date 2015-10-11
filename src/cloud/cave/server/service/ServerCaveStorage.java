package cloud.cave.server.service;

import cloud.cave.common.CaveStorageException;
import cloud.cave.domain.Direction;
import cloud.cave.domain.IMongoSetup;
import cloud.cave.domain.Region;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.Point3;
import cloud.cave.server.common.RoomRecord;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.CaveStorage;
import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.ne;


/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class ServerCaveStorage implements CaveStorage {
    public static final String COLLECTION_PLAYERS = "droogs";
    public static final String COLLECTION_ROOMS = "rooms";
    public static final String COLLECTION_MESSAGES = "messages";
    private static final Logger logger = LoggerFactory.getLogger(ServerCaveStorage.class);
    private ServerConfiguration config;
    private final IMongoSetup mongoSetup;

    public ServerCaveStorage() {
        this(new MongoConnectionSetup());
    }

    // http://mongodb.github.io/mongo-java-driver/3.0/driver/getting-started/quick-tour/
    public ServerCaveStorage(IMongoSetup mongoSetup) {
        this.mongoSetup = mongoSetup;
    }

    private void createBaseData() {
        List<String> messageList = new ArrayList<>();
        messageList.add("[Mark] First Like");
        messageList.add("[Rohde] OMG det sagde du bare ikk");
        messageList.add("[Mark] Jo, jeg fik First Like");

        RoomRecord entryRoom = new RoomRecord("You are standing at the end of a road before a small brick building.",
                new ArrayList<String>());
        this.addRoom(new Point3(0, 0, 0).getPositionString(), entryRoom);

        this.getRoom(new Point3(0, 0, 0).getPositionString()).getMessageList().addAll(messageList);

        //North
        messageList = new ArrayList<>();
        messageList.add("[Little Prince] Why are you drinking?");
        messageList.add("[Tippler] So that I may forget");
        messageList.add("[Little Prince] Forget what?");
        messageList.add("[Tippler] Forget that I am ashamed");
        messageList.add("[Little Prince] Ashamed of what?");
        messageList.add("[Tippler] Ashamed of drinking!");

        this.addRoom(new Point3(0, 1, 0).getPositionString(),
                new RoomRecord("You are in open forest, with a deep valley to one side.", new ArrayList<String>()));

        //East
        this.addRoom(new Point3(1, 0, 0).getPositionString(),
                new RoomRecord("You are inside a building, a well house for a large spring.", new ArrayList<String>()));
        //West
        this.addRoom(new Point3(-1, 0, 0).getPositionString(),
                new RoomRecord("You have walked up a hill, still in the forest.", new ArrayList<String>()));
        //Up
        this.addRoom(new Point3(0, 0, 1).getPositionString(),
                new RoomRecord("You are in the top of a tall tree, at the end of a road.", new ArrayList<String>()));

        this.getRoom(new Point3(0, 1, 0).getPositionString()).getMessageList().addAll(messageList);
    }

    private <T> T executeSafe(Delegate<T> delegate) {
        try {
            return delegate.run();
        } catch (MongoSocketReadException e) {
            throw new CaveStorageException(e.getMessage(), e);
        } catch (MongoSocketWriteException e) {
            throw new CaveStorageException(e.getMessage(), e);
        } catch (MongoSocketOpenException e) {
            throw new CaveStorageException(e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception while inserting into mongodb", e);
        }
    }

    private interface Delegate<T> {
        T run();
    }

    @Override
    public boolean addRoom(final String positionString, final RoomRecord roomRecord) {
        executeSafe(new Delegate<Void>() {
            @Override
            public Void run() {
                MongoCollection<Document> collection = mongoSetup.getCollection(COLLECTION_ROOMS);

                Document room = new Document()
                        .append("_id", positionString)
                        .append("description", roomRecord.description.trim());
                collection.insertOne(room);

                return null;
            }
        });


        return true;
    }

    @Override
    public RoomRecord getRoom(final String positionString) {
        return executeSafe(new Delegate<RoomRecord>() {
            @Override
            public RoomRecord run() {
                MongoCollection<Document> roomCollection = mongoSetup.getCollection(COLLECTION_ROOMS);
                Document room = roomCollection.find(new Document("_id", positionString)).first();
                logger.debug(positionString);
                if(room != null){
                    String description = room.getString("description");

                    List<String> messageList = new MongoMessageList(positionString);
                    return new RoomRecord(description, messageList);
                }
                return null;
            }
        });
    }

    @Override
    public List<Direction> getSetOfExitsFromRoom(final String positionString) {
        return executeSafe(new Delegate<List<Direction>>() {
            @Override
            public List<Direction> run() {
                MongoCollection<Document> roomCollection = mongoSetup.getCollection(COLLECTION_ROOMS);
                List<Direction> listOfExits = new ArrayList<>();
                Point3 pZero = Point3.parseString(positionString);
                Point3 p;
                for (Direction d : Direction.values()) {
                    p = new Point3(pZero.x(), pZero.y(), pZero.z());
                    p.translate(d);
                    String position = p.getPositionString();

                    if(roomCollection.count(new Document("_id", position)) > 0)
                        listOfExits.add(d);

                }
                return listOfExits;
            }
        });
    }

    @Override
    public PlayerRecord getPlayerByID(final String playerID) {
        return executeSafe(new Delegate<PlayerRecord>() {
            @Override
            public PlayerRecord run() {
                MongoCollection<Document> playerCollection = mongoSetup.getCollection(COLLECTION_PLAYERS);
                Document players = playerCollection.find(new Document("_id", playerID)).first();
                return documentToPlayerRecord(players);
            }
        });
    }

    @Override
    public void updatePlayerRecord(final PlayerRecord record) {
        executeSafe(new Delegate<Void>() {
            @Override
            public Void run() {
                MongoCollection<Document> playerCollection = mongoSetup.getCollection(COLLECTION_PLAYERS);
                Document player = new Document()
                        .append("_id", record.getPlayerID())
                        .append("playerName", record.getPlayerName())
                        .append("groupName", record.getGroupName())
                        .append("region", record.getRegion().toString())
                        .append("positionAsString", record.getPositionAsString())
                        .append("sessionID", record.getSessionId());
                playerCollection.replaceOne(new Document("_id", record.getPlayerID()), player, new UpdateOptions().upsert(true));
                return null;
            }
        });
    }

    @Override
    public List<PlayerRecord> computeListOfPlayersAt(final String positionString, final int offset) {
        return executeSafe(new Delegate<List<PlayerRecord>>() {
            @Override
            public List<PlayerRecord> run() {
                int start = 0;
                int limit = 10;

                if (offset > 0) {
                    start = 10 + (20*(offset-1));
                    limit = 20;
                }

                MongoCollection<Document> playerCollection = mongoSetup.getCollection(COLLECTION_PLAYERS);
                FindIterable<Document> playersAt = playerCollection.find(new Document("positionAsString", positionString))
                        .sort(Sorts.ascending("_id"))
                        .skip(start)
                        .limit(limit);
                final LinkedList <PlayerRecord> playersAtLocationList = new LinkedList<>();

                playersAt.forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        playersAtLocationList.add(documentToPlayerRecord(document));
                    }
                });

                return playersAtLocationList;
            }
        });
    }

    @Override
    public long computeCountOfActivePlayers() {
        return executeSafe(new Delegate<Long>() {
            @Override
            public Long run() {
                MongoCollection<Document> playerCollection = mongoSetup.getCollection(COLLECTION_PLAYERS);
                long activePlayers = playerCollection.count(ne("sessionID", null));

                return activePlayers;
            }
        });
    }

    @Override
    public void initialize(ServerConfiguration config) {
        this.config = config;
        this.mongoSetup.initialize(config);

        executeSafe(new Delegate<Void>() {
            @Override
            public Void run() {
                MongoCollection<Document> roomCollection = mongoSetup.getCollection(COLLECTION_ROOMS);
                if (roomCollection.count() == 0)
                    createBaseData();
                return null;
            }
        });
    }

    @Override
    public void disconnect() {
        this.mongoSetup.disconnect();
    }

    @Override
    public ServerConfiguration getConfiguration() {
        return this.config;
    }

    private PlayerRecord documentToPlayerRecord(Document document){
        PlayerRecord p = null;

        if(document != null){
            String playerID = document.getString("_id");
            String playerName = document.getString("playerName");
            String groupName = document.getString("groupName");
            String positionAsString = document.getString("positionAsString");
            Region region = Region.valueOf(document.getString("region"));
            String sessionID = document.getString("sessionID");


            p = new PlayerRecord(playerID, playerName, groupName, region, positionAsString, sessionID);
        }
        return p;
    }

    // Helper class for message list
    private class MongoMessageList implements List<String> {
        private String id;
        private List<String> lastRead;

        public MongoMessageList(String id) {
            this.id = id;
            this.lastRead = new ArrayList<>();
        }

        private List<String> getMessages() {
            return executeSafe(new Delegate<List<String>>() {
                @Override
                public List<String> run() {
                    MongoCollection<Document> messageCollection = mongoSetup.getCollection(COLLECTION_MESSAGES);
                    FindIterable<Document> messages = messageCollection.find(new Document("room", id))
                            .sort(Sorts.ascending("timestamp"));

                    final ArrayList<String> messageList = new ArrayList<>();
                    messages.forEach(new Block<Document>() {
                        @Override
                        public void apply(Document document) {
                            messageList.add(document.getString("message"));
                        }
                    });

                    lastRead = messageList;
                    return lastRead;
                }
            });
        }

        @Override
        public boolean add(final String s) {
            return executeSafe(new Delegate<Boolean>() {
                @Override
                public Boolean run() {
                    MongoCollection<Document> messageCollection = mongoSetup.getCollection(COLLECTION_MESSAGES);

                    Document message = new Document()
                            .append("room", id)
                            .append("message", s)
                            .append("timestamp", new Date().getTime());
                    messageCollection.insertOne(message);

                    return true;
                }
            });
        }

        @Override
        public String get(int i) {
            return getMessages().get(i);
        }

        @Override
        public int size() {
            return getMessages().size();
        }

        @Override
        public boolean isEmpty() {
            if (lastRead.isEmpty())
                return getMessages().isEmpty();
            else
                return false;
        }

        @Override
        public Iterator<String> iterator() {
            return getMessages().iterator();
        }

        @Override
        public Object[] toArray() {
            return getMessages().toArray();
        }

        @Override
        public ListIterator<String> listIterator() {
            return getMessages().listIterator();
        }

        @Override
        public ListIterator<String> listIterator(int i) {
            return getMessages().listIterator(i);
        }

        @Override
        public boolean contains(Object o) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            return getMessages().toArray(ts);
        }

        @Override
        public boolean remove(Object o) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean containsAll(Collection<?> collection) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean addAll(Collection<? extends String> collection) {
            for (String m : collection) {
                this.add(m);
            }
            return true;
        }

        @Override
        public boolean addAll(int i, Collection<? extends String> collection) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void clear() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String set(int i, String s) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void add(int i, String s) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String remove(int i) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public int indexOf(Object o) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public List<String> subList(int i, int i1) {
            throw new RuntimeException("Not implemented");
        }
    }
}
