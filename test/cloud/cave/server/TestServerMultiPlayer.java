package cloud.cave.server;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.*;

import cloud.cave.common.*;
import cloud.cave.domain.*;

/**
 * Testing that the cave can handle having multiple
 * players in the cave at the same time.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class TestServerMultiPlayer {

    private Cave cave;

    private Player p1, p2;

    @Before
    public void setup() {
        cave = CommonCaveTests.createTestDoubledConfiguredCave();
    }

    private void enterBothPlayers() {
        Login loginResult = cave.login("magnus_aarskort", "312");
        p1 = loginResult.getPlayer();

        loginResult = cave.login("mathilde_aarskort", "321");
        p2 = loginResult.getPlayer();
    }

    @Test
    public void shouldAllowIndependentMoves() {
        enterBothPlayers();

        // p1 moves west, only one left in entry room
        p1.move(Direction.WEST);

        // p2 moves east, none left in entry room
        p2.move(Direction.EAST);

        // The room descriptions are different
        assertNotEquals(p1.getShortRoomDescription(), p2.getShortRoomDescription());

        p2.move(Direction.WEST);
        assertNotEquals(p1.getShortRoomDescription(), p2.getShortRoomDescription());

        p2.move(Direction.WEST);
        assertEquals(p1.getShortRoomDescription(), p2.getShortRoomDescription());
    }

    @Test
    public void shouldSeeOtherPlayersInSameLocation() {
        enterBothPlayers();
        p1.move(Direction.WEST);

        // only myself
        assertEquals(1, p1.getPlayersHere(0).size());
        List<String> playersHere;
        playersHere = p1.getPlayersHere(0);
        assertThat(playersHere, hasItem(p1.getName()));

        // move p2 there
        p2.move(Direction.WEST);
        assertEquals(2, p2.getPlayersHere(0).size());
        playersHere = p2.getPlayersHere(0);
        assertThat(playersHere, hasItems(p1.getName(), p2.getName()));
    }

    @Test
    public void shouldProvideBothPlayersInLongDescription() {
        enterBothPlayers();
        String longDescription = p1.getLongRoomDescription(0);
        // System.out.println(longDescription);

        assertThat(longDescription, containsString("You see other players:"));
        assertThat(longDescription, containsString("Magnus"));
        assertThat(longDescription, containsString("Mathilde"));

        // move mathilde out of the room
        p2.move(Direction.WEST);
        longDescription = p1.getLongRoomDescription(0);

        assertThat(longDescription, containsString("Magnus"));
        assertThat(longDescription, not(containsString("Mathilde")));
    }

    @Test
    public void shouldSeeLongDescriptionUpdateWhenOtherPlayerLogOut() {
        enterBothPlayers();
        CommonCaveTests.shouldAllowLoggingOutMagnus(cave, p1);
        String longDescription = p2.getLongRoomDescription(0);
        // System.out.println(longDescription);

        assertThat(longDescription, containsString("You see other players:"));
        assertThat(longDescription, not(containsString("Magnus")));
        assertThat(longDescription, containsString("Mathilde"));
    }

    @Test
    public void shouldSeePlayersInRoom() {
        Login loginResult = cave.login("magnus_aarskort", "312");
        p1 = loginResult.getPlayer();

        CommonPlayerTests.shouldSeeMathildeComingInAndOutOfRoomDuringSession(cave, p1);
    }

    private List<Player> doMassLogin() {
        List<Player> players = new LinkedList<>();

        Login loginResult;

        loginResult = cave.login("rwar400t", "727b9c");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar401t", "ynizl2");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar402t", "f0s4p3");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar403t", "plcs74");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar404t", "v76ifd");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar405t", "jxe9ha");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar406t", "6xp9jl");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar407t", "u3mxug");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar408t", "trv9gy");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar409t", "1d5fh3");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar410t", "zsafci");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar411t", "v324q6");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar412t", "2jdfhz");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar413t", "zja3ig");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar414t", "04nj10");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar415t", "zu5qar");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar416t", "qildw2");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar417t", "61w8sh");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar418t", "exwt5w");
        players.add(loginResult.getPlayer());
        loginResult = cave.login("rwar419t", "n7lzqw");
        players.add(loginResult.getPlayer());

        return players;
    }

    @Test
    public void shouldShowOnlyBoundedListOfPlayers() {
        enterBothPlayers();
        doMassLogin();

        String longRoomDescription = p1.getLongRoomDescription(0);
        String[] descriptionParts = longRoomDescription.split("\n");
        String[] playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(10));

        longRoomDescription = p1.getLongRoomDescription(1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(13));
        assertThat(playerParts[12], is(" *** End of player list *** "));

        // Default behavior is to show just first 10
        longRoomDescription = p1.getLongRoomDescription(-1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(10));
    }

    @Test
    public void shouldShowEndOfListWithFewPlayers() {
        enterBothPlayers();

        String longRoomDescription = p1.getLongRoomDescription(0);
        String[] descriptionParts = longRoomDescription.split("\n");
        String[] playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(3));
        assertThat(playerParts[2], is(" *** End of player list *** "));
    }
}


