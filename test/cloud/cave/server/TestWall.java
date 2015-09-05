package cloud.cave.server;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
//import static org.hamcrest.CoreMatchers.*;

import cloud.cave.server.common.RoomRecord;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;

/**
 * Initial template of TDD of students' exercises
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class TestWall {

    private RoomRecord roomRecord;

    private Player playerMikkel;
    private Player playerMathilde;

    @Before
    public void setUp() throws Exception {
        Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();

        Login loginResult = cave.login("mikkel_aarskort", "123");
        playerMikkel = loginResult.getPlayer();
        loginResult = cave.login("mathilde_aarskort", "321");
        playerMathilde = loginResult.getPlayer();


    }


    @Test
    public void shouldWriteToAndReadWall() {
        playerMikkel.move(Direction.EAST);
        playerMikkel.addMessage("This is message no. 1");
        List<String> wallContents = playerMikkel.getMessageList();

        // TODO: Exercise - solve the 'wall' exercise

        assertThat(wallContents.size(), is(1));
        assertThat(wallContents.get(0), containsString("This is message no. 1"));
        assertThat(wallContents.get(0), containsString("Mikkel"));
    }

    @Test
    public void shouldReadFirstMessage() {
        assertThat(playerMikkel.getMessageList().get(0), is("[Mark] First Like"));

    }

    @Test
    public void shouldReadFirstThreeMessages() {
        List<String> wallContents = playerMikkel.getMessageList();

        assertThat(wallContents.size(), is(3));
        assertThat(wallContents.get(0), is("[Mark] First Like"));
        assertThat(wallContents.get(1), is("[Rohde] OMG det sagde du bare ikk"));
        assertThat(wallContents.get(2), is("[Mark] Jo, jeg fik First Like"));

    }

    @Test
    public void shouldReturnMikkelAndMathildeAtSameRoom() {
        String mikkelPosition = playerMikkel.getPosition();
        String mathildePosition = playerMathilde.getPosition();

        assertThat(mikkelPosition, is(mathildePosition));
        assertThat(playerMikkel.getMessageList(), equalTo(playerMathilde.getMessageList()));

    }

    @Test
    public void shouldReturnMikkelAndMathildeAtDifferentRoomsWithDifferentMessageLists() {
        playerMathilde.move(Direction.NORTH);
        assertThat(playerMikkel.getMessageList(), not(equalTo(playerMathilde.getMessageList())));
    }

    @Test
    public void shouldReturnMikkelAndMathildeAtSameRoomWithSameMessageListsAfterMovement() {
        playerMathilde.move(Direction.NORTH);
        assertThat(playerMikkel.getMessageList(), not(equalTo(playerMathilde.getMessageList())));

        playerMathilde.move(Direction.SOUTH);
        assertThat(playerMikkel.getMessageList(), equalTo(playerMathilde.getMessageList()));
    }

    @Test
    public void shouldReturnTipplerDialogAfterMovingNorth() {

        playerMathilde.move(Direction.NORTH);
        List<String> messageList = new ArrayList<>();
        messageList.add("[Little Prince] Why are you drinking?");
        messageList.add("[Tippler] So that I may forget");
        messageList.add("[Little Prince] Forget what?");
        messageList.add("[Tippler] Forget that I am ashamed");
        messageList.add("[Little Prince] Ashamed of what?");
        messageList.add("[Tippler] Ashamed of drinking!");

        assertThat(playerMathilde.getMessageList(), equalTo(messageList));
    }


}
