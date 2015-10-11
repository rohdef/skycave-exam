package cloud.cave.server.service;

import cloud.cave.common.CaveStorageException;
import cloud.cave.domain.Region;
import cloud.cave.doubles.mongo.FakeMongoSetup;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.server.common.RoomRecord;
import cloud.cave.server.common.ServerConfiguration;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import com.mongodb.MongoSocketWriteException;
import com.mongodb.ServerAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestServerCaveStorage {
    private ServerCaveStorage caveStorage;
    private FakeMongoSetup mongoSetup;

    @Before
    public void setup() {
        mongoSetup = new FakeMongoSetup();
        caveStorage = new ServerCaveStorage(mongoSetup);
    }

    @After
    public void tearDown() {
        caveStorage.disconnect();
    }

    private interface IDelegate {
        void run();
    }

    private void performExceptions(IDelegate delegate) {
        Random random = new Random();
        int randomValue = random.nextInt(3);
        switch (randomValue) {
            case 0:
                mongoSetup.setNextException(new MongoSocketReadException("Prematurely reached end of stream",
                        new ServerAddress("db0")));
                break;
            case 1:
                mongoSetup.setNextException(new MongoSocketWriteException("Prematurely reached end of stream",
                        new ServerAddress("db0"), new RuntimeException()));
                break;
            case 2:
                mongoSetup.setNextException(new MongoSocketOpenException("Prematurely reached end of stream",
                        new ServerAddress("db0"), new RuntimeException()));
                break;
        }

        try {
            delegate.run();
            fail("Expected a CaveStorageException, but no exception was thrown");
        } catch (CaveStorageException e) {
            assertThat(true, is(true));
        } catch (Exception e) {
            fail("Expected a CaveStorageException, but got this exception in stead: " + e.getClass().toString()
                    + ", with the following message: " + e.getMessage());
        }
    }

    @Test
    public void shouldHandleExceptionsOnAddRoom() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.addRoom("(0,0,0)", new RoomRecord("Flubberduck", new LinkedList<String>()));
            }
        });
    }

    @Test
    public void shouldAddRoomWhenNoException() {
        RoomRecord roomRecord = new RoomRecord("Flubberduck", new LinkedList<String>());
        assertThat(caveStorage.addRoom("(0,0,0)", roomRecord), is(true));
    }

    @Test
    public void shouldHandleExceptionsOnGetRoom() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.getRoom("(0,0,0)");
            }
        });
    }

    @Test
    public void shouldGetRoomWhenNoException() {
        RoomRecord roomRecord = caveStorage.getRoom("(0,0,0)");
        assertThat(roomRecord, is(notNullValue()));
        assertThat(roomRecord.getMessageList(), is(notNullValue()));
        assertThat(roomRecord.getMessageList().get(0), is(notNullValue()));
        assertThat(roomRecord.getMessageList().iterator(), is(notNullValue()));
        assertThat(roomRecord.getMessageList().toArray(), is(notNullValue()));
        assertThat(roomRecord.getMessageList().size(), is(3));
        assertThat(roomRecord.getMessageList().isEmpty(), is(false));
    }

    @Test
    public void shouldHandleExceptionsOnRoomMessages() {
        final RoomRecord roomRecord = caveStorage.getRoom("(0,0,0)");

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().get(0);
            }
        });

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().iterator();
            }
        });

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().toArray();
            }
        });

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().size();
            }
        });

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().isEmpty();
            }
        });
    }

    @Test
    public void addToMessagesWhenNoException() {
        final RoomRecord roomRecord = caveStorage.getRoom("(0,0,0)");

        assertThat(roomRecord.getMessageList().add("Foo"), is(true));
        assertThat(roomRecord.getMessageList().addAll(Arrays.asList("foo", "bar", "baz")), is(true));
    }

    @Test
    public void shouldHandleExceptionsOnAddMessages() {
        final RoomRecord roomRecord = caveStorage.getRoom("(0,0,0)");

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().add("Foo");
            }
        });

        performExceptions(new IDelegate() {
            @Override
            public void run() {
                roomRecord.getMessageList().addAll(Arrays.asList("foo", "bar", "baz"));
            }
        });
    }

    @Test
    public void shouldntGetNonexistentRoomWhenNoException() {
        assertThat(caveStorage.getRoom("null"), is(nullValue()));
    }

    @Test
    public void shouldHandleExceptionsOnGetSetOfExitsFromRoom() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.getSetOfExitsFromRoom("(0,0,0)");
            }
        });
    }

    @Test
    public void shouldGetSetOfExitsFromRoomWhenNoException() {
        assertThat(caveStorage.getSetOfExitsFromRoom("(0,0,0)"), is(notNullValue()));
    }

    @Test
    public void shouldHandleExceptionsOnGetPlayerByID() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.getPlayerByID("cheshire cat");
            }
        });
    }

    @Test
    public void shouldGetPlayerByIDWhenNoException() {
        assertThat(caveStorage.getPlayerByID("rabbit"), is(notNullValue()));
    }

    @Test
    public void shouldHandleExceptionsOnUpdatePlayerRecord() {
        final PlayerRecord playerRecord = new PlayerRecord("hatter", "The Mad Hatter", "neutral", Region.AALBORG, "tea table", "Why does");
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.updatePlayerRecord(playerRecord);
            }
        });
    }

    @Test
    public void shouldUpdatePlayerRecordWhenNoException() {
        final PlayerRecord playerRecord = new PlayerRecord("hatter", "The Mad Hatter", "neutral", Region.AALBORG, "tea table", "Why does");
        try {
            caveStorage.updatePlayerRecord(playerRecord);
            assertThat(true, is(true));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void shouldHandleExceptionsOnComputeListOfPlayersAt() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.computeListOfPlayersAt("tea table", -1);
            }
        });
    }

    @Test
    public void shouldComputeListOfPlayersAtWhenNoException() {
        assertThat(caveStorage.computeListOfPlayersAt("wonderland", -1), is(notNullValue()));
    }

    @Test
    public void shouldHandleExceptionsOnComputeCountOfActivePlayers() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.computeCountOfActivePlayers();
            }
        });
    }

    @Test
    public void shouldComputeCountOfActivePlayersWhenNoException() {
        assertThat(caveStorage.computeCountOfActivePlayers(), is(notNullValue()));
    }

    ServerConfiguration config = new ServerConfiguration("foo", 42);
    @Test
    public void shouldHandleExceptionsInitialize() {
        performExceptions(new IDelegate() {
            @Override
            public void run() {
                caveStorage.initialize(config);
            }
        });
    }

    @Test
    public void shouldInitializeWhenNoException() {
        caveStorage.initialize(config);
        assertThat(caveStorage.getConfiguration().get(0).getHostName(), is ("foo"));
        assertThat(caveStorage.getConfiguration().get(0).getPortNumber(), is (42));
    }
}
