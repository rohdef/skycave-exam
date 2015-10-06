package cloud.cave.doubles;

import java.util.*;

import cloud.cave.domain.*;
import cloud.cave.server.common.*;
import cloud.cave.service.*;

/**
 * Fake object implementation of storage. Map data structures are used to
 * simulate SQL tables or NoSQL collections. The used data structures are not
 * synchronized.
 * <p/>
 * The cave is initialized with five rooms in a fixed layout, vaguely inspired
 * by the original Colossal Cave layout. These rooms serve the test cases.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class FakeCaveStorage implements CaveStorage {

    // The table/collection of rooms in the cave.
    // The positionString is the primary key and the value object
    // for a room the rest of the tuple
    Map<String, RoomRecord> roomMap;

    @Override
    public void initialize(ServerConfiguration config) {
        this.serverConfiguration = config;
        roomMap = new HashMap<>();
        playerId2PlayerSpecs = new HashMap<>(5);

        List<String> messageList = new ArrayList<>();
        messageList.add("[Mark] First Like");
        messageList.add("[Rohde] OMG det sagde du bare ikk");
        messageList.add("[Mark] Jo, jeg fik First Like");

        RoomRecord entryRoom = new RoomRecord("You are standing at the end of a road before a small brick building.",
                messageList);
        this.addRoom(new Point3(0, 0, 0).getPositionString(), entryRoom);

        //North
        messageList = new ArrayList<>();
        messageList.add("[Little Prince] Why are you drinking?");
        messageList.add("[Tippler] So that I may forget");
        messageList.add("[Little Prince] Forget what?");
        messageList.add("[Tippler] Forget that I am ashamed");
        messageList.add("[Little Prince] Ashamed of what?");
        messageList.add("[Tippler] Ashamed of drinking!");

        this.addRoom(new Point3(0, 1, 0).getPositionString(),
                new RoomRecord("You are in open forest, with a deep valley to one side.", messageList));

        //East
        this.addRoom(new Point3(1, 0, 0).getPositionString(),
                new RoomRecord("You are inside a building, a well house for a large spring.", new ArrayList<String>()));
        //West
        this.addRoom(new Point3(-1, 0, 0).getPositionString(),
                new RoomRecord("You have walked up a hill, still in the forest.", new ArrayList<String>()));
        //Up
        this.addRoom(new Point3(0, 0, 1).getPositionString(),
                new RoomRecord("You are in the top of a tall tree, at the end of a road.", new ArrayList<String>()));
    }

    @Override
    public void disconnect() {
        roomMap = null;
    }

    @Override
    public RoomRecord getRoom(String positionString) {
        return roomMap.get(positionString);
    }

    @Override
    public boolean addRoom(String positionString, RoomRecord newRoom) {
        // if there is already a room, return false
        if (roomMap.containsKey(positionString)) {
            return false;
        }
        roomMap.put(positionString, newRoom);
        return true;
    }

    @Override
    public List<Direction> getSetOfExitsFromRoom(String positionString) {
        List<Direction> listOfExits = new ArrayList<>();
        Point3 pZero = Point3.parseString(positionString);
        Point3 p;
        for (Direction d : Direction.values()) {
            p = new Point3(pZero.x(), pZero.y(), pZero.z());
            p.translate(d);
            String position = p.getPositionString();
            if (roomMap.containsKey(position)) {
                listOfExits.add(d);
            }
        }
        return listOfExits;
    }

    // === The table with primary key playerID whose columns are the
    // specifications of a given player. The private datastructure PlayerSpecs
    // represents the
    // remaining tuple values.

    Map<String, PlayerRecord> playerId2PlayerSpecs;

    @Override
    public PlayerRecord getPlayerByID(String playerID) {
        PlayerRecord ps = playerId2PlayerSpecs.get(playerID);
        return ps;
    }

    @Override
    public void updatePlayerRecord(PlayerRecord record) {
        playerId2PlayerSpecs.put(record.getPlayerID(), record);
    }

    @Override
    public List<PlayerRecord> computeListOfPlayersAt(String positionString, int offset) {
        int start = 0;
        int max = 10;

        if (offset > 0) {
            start = 10 + (20*(offset-1));
            max = 10 + (20*(offset));
        }

        List<PlayerRecord> theList = new ArrayList<>();
        for (String id : playerId2PlayerSpecs.keySet()) {
            PlayerRecord ps = playerId2PlayerSpecs.get(id);
            if (ps.isInCave() && ps.getPositionAsString().equals(positionString)) {
                theList.add(ps);
            }
        }

        // Safety cap to keep within bounds
        if (theList.size() == 0 || theList.size() <= start) return new ArrayList<>();
        max = Math.min(theList.size(), max);

        return theList.subList(start, max);
    }

    @Override
    public long computeCountOfActivePlayers() {
        return getPlayerList().size();
    }

    /**
     * Compute the list of players in the cave.
     *
     * @return list of all players in the cave.
     */
    private List<PlayerRecord> getPlayerList() {
        List<PlayerRecord> theList =
                new ArrayList<>();
        for (String id : playerId2PlayerSpecs.keySet()) {
            PlayerRecord ps = playerId2PlayerSpecs.get(id);
            if (ps.isInCave()) {
                theList.add(ps);
            }
        }
        return theList;
    }

    /**
     * Debugging method to dump contents of internal data structures
     *
     * @return the contents of the map(player id, player records)
     */
    public String dumpContents() {
        return "FakeStorage: " + playerId2PlayerSpecs + ".";
    }

    public String toString() {
        return "FakeCaveStorage (Fake Object implementation of in-memory storage)";
    }

    ServerConfiguration serverConfiguration;

    @Override
    public ServerConfiguration getConfiguration() {
        return serverConfiguration;
    }

}
