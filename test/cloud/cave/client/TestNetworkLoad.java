package cloud.cave.client;

import cloud.cave.server.common.Point3;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;
import cloud.cave.doubles.*;
import cloud.cave.ipc.*;
import cloud.cave.server.StandardInvoker;

/**
 * A hint on how network load can be monitored using
 * a spy - without any real networking occuring.
 * <p/>
 * These tests will likely fail if you change any
 * marshaling code!
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class TestNetworkLoad {
    private PlayerProxy player;
    private LoadSpyClientRequestHandler spy;

    @Before
    public void setUp() throws Exception {
        // Create the server tier
        Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();

        // create the invoker on the server side, bind it to the cave
        Invoker srh = new StandardInvoker(cave);

        // create the client request handler as a test double that
        // simply uses method calls to call the 'server side'
        ClientRequestHandler crh = new LocalMethodCallClientRequestHandler(srh);
        spy = new LoadSpyClientRequestHandler(crh);

        // Create the cave proxy, and login mikkel
        Cave caveProxy = new CaveProxy(spy);
        Login loginResult = caveProxy.login("mikkel_aarskort", "123");

        player = (PlayerProxy) loginResult.getPlayer();
    }

    @Test
    public void shouldVerifyBytesSentOverNetwork() {
        spy.reset();
        player.getLongRoomDescription(0);
        // Verify the amount of bytes sent and received
        assertThat(spy.getSent(), is(157));
        assertThat(spy.getReived(), is(264));

        spy.reset();
        player.getLongRoomDescription(2000000000);
        assertThat(spy.getSent(), is(166));
    }

    @Test
    public void shouldCacheWeather() throws InterruptedException {
        player.setWeatherTimeout(1000);
        String weather1, weather2;

        spy.reset();
        weather1 = player.getWeather();
        assertThat(spy.getSent(), is(greaterThan(0)));
        assertThat(spy.getReived(), is(greaterThan(0)));

        spy.reset();
        weather2 = player.getWeather();
        assertThat(spy.getSent(), is(0));
        assertThat(spy.getReived(), is(0));
        assertThat(weather1, is(weather2));

        Thread.sleep(500);

        spy.reset();
        weather2 = player.getWeather();
        assertThat(spy.getSent(), is(0));
        assertThat(spy.getReived(), is(0));
        assertThat(weather1, is(weather2));

        Thread.sleep(400);
        spy.reset();
        weather2 = player.getWeather();
        assertThat(spy.getSent(), is(0));
        assertThat(spy.getReived(), is(0));
        assertThat(weather1, is(weather2));

        Thread.sleep(200);
        spy.reset();
        player.getWeather();
        assertThat(spy.getSent(), is(greaterThan(0)));
        assertThat(spy.getReived(), is(greaterThan(0)));
    }

    @Test
    public void shouldNotHaveTrafficOnKnownValues() {
        testTraffic();;

        player.move(Direction.EAST);
        testTraffic();

        player.execute("HomeCommand", "bar");
        testTraffic();
    }

    private void testTraffic() {
        spy.reset();
        final String description = player.getShortRoomDescription();
        assertThat(spy.getSent(), is(0));
        assertThat(spy.getReived(), is(0));
        assertThat(description, is(notNullValue()));
        assertThat(description.length(), is(greaterThan(0)));

        spy.reset();
        final String position = player.getPosition();
        assertThat(spy.getSent(), is(0));
        assertThat(spy.getReived(), is(0));
        assertThat(position, is(notNullValue()));
        assertThat(position.length(), is(greaterThan(0)));
        assertThat(Point3.parseString(position), is(notNullValue()));
    }
}
