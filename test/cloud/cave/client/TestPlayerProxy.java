package cloud.cave.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.*;

import cloud.cave.common.*;
import cloud.cave.domain.*;
import cloud.cave.doubles.*;
import cloud.cave.ipc.*;
import cloud.cave.server.*;

/**
 * Test that the PlayerClientProxy allows all behaviour defined by the Player
 * interface to be successfully communicated with the server tier.
 * <p/>
 * <p/>
 * Note: Most of these tests are naturally identical for the tests of the server
 * player implementation.
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class TestPlayerProxy {

    private PlayerProxy playerProxy;
    private ClientRequestHandler crh;
    private Cave caveProxy;

    @Before
    public void setUp() throws Exception {
        // Create the server tier
        Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();

        // create the invoker on the server side, bind it to the cave
        Invoker srh = new StandardInvoker(cave);

        // create the client request handler as a test double that
        // simply uses method calls to call the 'server side'
        crh = new LocalMethodCallClientRequestHandler(srh);

        // Create the cave proxy, and login mikkel
        caveProxy = new CaveProxy(crh);
        Login loginResult = caveProxy.login("mikkel_aarskort", "123");

        playerProxy = (PlayerProxy) loginResult.getPlayer();
    }

    @Test
    public void shouldReadMessagesInRoom() {
        List<String> messageList = playerProxy.getMessageList();

        // If size, the first and last message matches, assume that it's working and ordered
        assertThat(messageList.size(), is(3));
        assertThat(messageList.get(0), containsString("First Like"));
        assertThat(messageList.get(2), containsString("Jo, jeg fik First Like"));

        // Move and repeat
        playerProxy.move(Direction.NORTH);
        messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(6));
        assertThat(messageList.get(0), containsString("Why are you drinking?"));
        assertThat(messageList.get(5), containsString("Ashamed of drinking!"));
    }

    @Test
    public void shouldAddMessagesInRoom() {
        List<String> messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(3));

        playerProxy.addMessage("Du ka da se du ik var nummer 1");
        messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(4));
        assertThat(messageList.get(3), containsString("Du ka da se du ik var nummer 1"));

        playerProxy.addMessage("Christopher, synes du ik bare det irriterende, at andre siger du er bøsse og klam " +
                "og et svin for du er overhovedet ingen af delene");
        messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(5));
        assertThat(messageList.get(4), containsString("Christopher, synes du ik bare det irriterende, at andre " +
                "siger du er bøsse og klam og et svin for du er overhovedet ingen af delene"));

        playerProxy.move(Direction.EAST);
        messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(0));

        playerProxy.addMessage("Two things are infinite: the universe and human stupidity; and I'm not sure about the universe.");
        messageList = playerProxy.getMessageList();

        assertThat(messageList.size(), is(1));
        assertThat(messageList.get(0), containsString("Two things are infinite: the universe and human stupidity; and I'm not sure about the universe."));
    }

    @Test
    public void shouldAccessSimpleAttributes() {
        CommonPlayerTests.shouldAccessSimpleAttributes(playerProxy);
    }

    @Test
    public void shouldAllowMovingEast() {
        boolean isLegal = playerProxy.move(Direction.EAST);
        JSONObject req = playerProxy.lastSentRequestObject();
        assertEquals(36, req.get(MarshalingKeys.PLAYER_SESSION_ID_KEY).toString().length());
        assertEquals(MarshalingKeys.MOVE_METHOD_KEY, req.get(MarshalingKeys.METHOD_KEY));
        assertEquals("EAST", req.get(MarshalingKeys.PARAMETER_HEAD_KEY));

        assertTrue("Moving east should be legal", isLegal);
    }

    @Test
    public void shouldGetShortRoomDescription() {
        String description = playerProxy.getShortRoomDescription();
        assertTrue("Initial location missing proper description",
                description.contains("You are standing at the end of a road before a small brick building."));
    }

    @Test
    public void shouldGetPosition() {
        CommonPlayerTests.shouldTestCoordinateTranslations(playerProxy);
    }

    @Test
    public void shouldHandleRemoteMoveAndDescription() {
        // move east
        shouldAllowMovingEast();
        String description = playerProxy.getShortRoomDescription();
        // System.out.println(description);
        assertTrue("The (remote) move has not moved the player",
                description.contains("You are inside a building, a well house for a large spring."));
    }

    @Test
    public void shouldProvideLongDescription() {
        CommonPlayerTests.shouldProvideLongDescription(playerProxy);
    }

    @Test
    public void shouldAllowPlayerToDigNewRooms() {
        CommonPlayerTests.shouldAllowPlayerToDigNewRooms(playerProxy);
    }

    @Test
    public void shouldNotAllowDigAtEast() {
        CommonPlayerTests.shouldNotAllowDigAtEast(playerProxy);
    }

    @Test
    public void shouldShowExitsForPlayersPosition() {
        CommonPlayerTests.shouldShowExitsForPlayersPosition(playerProxy);
    }

    @Test
    public void shouldSeePlayersInRoom() {
        CommonPlayerTests.shouldSeeMathildeComingInAndOutOfRoomDuringSession(caveProxy, playerProxy);
    }

    @Test
    public void shouldShowValidExitsFromEntryRoom() {
        CommonPlayerTests.shouldGetProperExitSet(playerProxy);
    }

    /**
     * Try to make an illformed request (not using the proxy)
     * and ensure that the server invoker makes an
     * appropriate reply.
     *
     * @throws CaveIPCException
     */
    @Test
    public void shouldReplyWithErrorInCaseRequestIsMalformed() throws CaveIPCException {
        JSONObject invalidRequest =
                Marshaling.createRequestObject(playerProxy.getID(), playerProxy.getSessionID(), "invalid-method-key", "my best parameter");
        JSONObject reply = null;

        reply = crh.sendRequestAndBlockUntilReply(invalidRequest);

        String errorCode = reply.get(MarshalingKeys.ERROR_CODE_KEY).toString();

        assertThat(errorCode, is(StatusCode.SERVER_UNKNOWN_METHOD_FAILURE));

        assertThat( //"The invalid request should tell where the error is",
                reply.get(MarshalingKeys.ERROR_MSG_KEY).toString(),
                containsString("StandardInvoker.handleRequest: Unknown dispatcher for the methodKey: invalid-method-key. Full request"));
    }

    @Test
    public void shouldBeAtPositionOfLastLogout() {
        // Log mathilde into the cave, initial position is 0,0,0
        // as the database is reset
        Login loginResult = caveProxy.login("mathilde_aarskort", "321");
        Player p1 = loginResult.getPlayer();

        CommonPlayerTests.shouldBeAtPositionOfLastLogout(caveProxy, p1);
    }

    @Test
    public void shouldValidateToString() {
        assertThat(playerProxy.toString(), is("(PlayerClientProxy: user-001/Mikkel)"));
    }

    @Test
    public void shouldHaveSessionIdAssigned() {
        // The session id is a UUID which is 36 characters long.
        assertThat(playerProxy.getSessionID().length(), is(36));
    }

    Player p1, p2;

    private void enterBothPlayers() {
        Login loginResult = caveProxy.login("magnus_aarskort", "312");
        p1 = loginResult.getPlayer();

        loginResult = caveProxy.login("mathilde_aarskort", "321");
        p2 = loginResult.getPlayer();
    }

    // Test that if a second client connects using the
    // same credentials as a first client is already
    // connected with, then the first client is
    // prevented from any actions ("disconnected" in
    // a sense). This is similar to the behavior of
    // Blizzard games (which is probably the standard).
    private List<Player> doMassLogin() {
        List<Player> players = new LinkedList<>();

        Login loginResult;

        loginResult = caveProxy.login("rwar400t", "727b9c");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar401t", "ynizl2");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar402t", "f0s4p3");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar403t", "plcs74");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar404t", "v76ifd");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar405t", "jxe9ha");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar406t", "6xp9jl");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar407t", "u3mxug");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar408t", "trv9gy");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar409t", "1d5fh3");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar410t", "zsafci");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar411t", "v324q6");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar412t", "2jdfhz");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar413t", "zja3ig");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar414t", "04nj10");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar415t", "zu5qar");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar416t", "qildw2");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar417t", "61w8sh");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar418t", "exwt5w");
        players.add(loginResult.getPlayer());
        loginResult = caveProxy.login("rwar419t", "n7lzqw");
        players.add(loginResult.getPlayer());

        return players;
    }

    @Test
    public void shouldShowPlayerListBoundedAndResetAtEnd() {
        enterBothPlayers();
        doMassLogin();

        String longRoomDescription = p1.getLongRoomDescription(0);
        String[] descriptionParts = longRoomDescription.split("\n");
        String[] playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(10));

        longRoomDescription = p1.getLongRoomDescription(1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(14));

        // Default behavior is to show just first 10
        p1.addMessage("Hello world");
        longRoomDescription = p1.getLongRoomDescription(-1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(10));

        longRoomDescription = p1.getLongRoomDescription(-1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(14));

        // We expect it to restart
        longRoomDescription = p1.getLongRoomDescription(-1);
        descriptionParts = longRoomDescription.split("\n");
        playerParts = Arrays.copyOfRange(descriptionParts, 4, descriptionParts.length);
        assertThat(playerParts.length, is(10));
    }

    @Test
    public void shouldPreventCallsFromDualLogins() {
        enterBothPlayers();
        p2.move(Direction.EAST);

        // log in Mathilde a second time
        Login loginResult = caveProxy.login("mathilde_aarskort", "321");
        assertThat(loginResult.getResultCode(), is(LoginResult.LOGIN_SUCCESS_PLAYER_ALREADY_LOGGED_IN));

        Player p2second = loginResult.getPlayer();

        // just precautions - we have two objects representing same player, right?
        assertNotEquals(p2, p2second);

        // Verify that the second client logged in is in the same
        // room as the the first client moved to
        assertThat(p2second.getPosition(), is("(1,0,0)"));

        // Verify that the first client CANNOT move mathilde west even
        // though the topology of the cave would allow it, instead
        // throws an Exception
        try {
            p2.move(Direction.WEST);
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }

        // assert that the second session IS allowed to do it
        assertThat(p2second.move(Direction.WEST), is(true));

        // Verify the other methods that do not cache things locally - sigh - cannot avoid a lot of duplicated code...
        try {
            p2.getLongRoomDescription(0);
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }

        try {
            p2.getExitSet();
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }

        try {
            p2.getPlayersHere(0);
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }

        try {
            p2.getLongRoomDescription(-1);
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }

        try {
            p2.digRoom(Direction.DOWN, "You are in the wonder room");
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }
        try {
            p2.execute("HomeCommand", "");
            fail("The first client must throw an exception when attempting any further calls");
        } catch (PlayerSessionExpiredException e) {
            assertThat(e.getMessage(), containsString("The session for player user-003 is no longer valid"));
        }
    }
}
