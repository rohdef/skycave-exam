package cloud.cave.server;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.doubles.WeatherServiceRequestFake;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.server.service.ServerWeatherService;
import org.json.simple.JSONObject;
import org.junit.*;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;
import cloud.cave.service.WeatherService;

/**
 * TDD Implementation of the weather stuff - initial steps.
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class TestWeather {

    private Cave cave;
    private Player player;

    @Before
    public void setUp() throws Exception {
        cave = CommonCaveTests.createTestDoubledConfiguredCave();
        String loginName = "mikkel_aarskort";
        Login loginResult = cave.login(loginName, "123");
        player = loginResult.getPlayer();
    }

    @Test
    public void shouldGetWeatherServerSide() {
        String weather = player.getWeather();
        assertThat(weather, containsString("The weather in AARHUS is Partly Cloudy, temperature 42.0C (feelslike 24.0C). Wind: 4.0 m/s, direction NNE."));
        assertThat(weather, containsString("This report is dated: Tue, 08 Sep 2015 13:24:22 +0200"));
    }

    @Test
    public void shouldRejectUnknownPlayer() {
        // Test the raw weather service api for unknown players
        CaveServerFactory factory = new AllTestDoubleFactory();
        WeatherService ws = factory.createWeatherServiceConnector();
        JSONObject json = ws.requestWeather("grp02", "user-003", Region.COPENHAGEN);
        assertThat(json.get("authenticated").toString(), is("false"));
        assertThat(json.get("errorMessage").toString(), is("GroupName grp02 or playerID user-003 is not authenticated."));

        // Try it using the full api
        Login loginResult = cave.login("mathilde_aarskort", "321");
        player = loginResult.getPlayer();
        assertNotNull("The player should have been logged in", player);

        String weather = player.getWeather();
        assertThat(weather, containsString("The weather service failed with message: GroupName grp02 or playerID user-003 is not authenticated"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectZeroSecondsDelay() {
        RequestSaboteur fakeRequest = new RequestSaboteur(new WeatherServiceRequestFake());
        ServerWeatherService ws = new ServerWeatherService(fakeRequest);
        ws.setSecondsDelay(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectMinusOneSecondsDelay() {
        RequestSaboteur fakeRequest = new RequestSaboteur(new WeatherServiceRequestFake());
        ServerWeatherService ws = new ServerWeatherService(fakeRequest);
        ws.setSecondsDelay(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectNegativeSecondsDelay() {
        RequestSaboteur fakeRequest = new RequestSaboteur(new WeatherServiceRequestFake());
        ServerWeatherService ws = new ServerWeatherService(fakeRequest);
        ws.setSecondsDelay(-18342);
    }
}
