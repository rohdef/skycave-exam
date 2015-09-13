package cloud.cave.client;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.service.WeatherService;
import org.junit.Before;
import org.junit.Test;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.Cave;
import cloud.cave.domain.Login;
import cloud.cave.doubles.LocalMethodCallClientRequestHandler;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.StandardInvoker;

import java.io.IOException;

/**
 * Testing the weather method on the
 * client side
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class TestWeatherClient {
    private PlayerProxy player;
    private WeatherService weatherService;

    @Before
    public void setUp() throws Exception {
        // Create the server tier
        AllTestDoubleFactory factory = CommonCaveTests.getFactory();
        Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();
        weatherService = factory.getLastWeatherService();

        // create the invoker on the server side, bind it to the cave
        Invoker srh = new StandardInvoker(cave);

        // create the client request handler as a test double that
        // simply uses method calls to call the 'server side'
        LocalMethodCallClientRequestHandler crh = new LocalMethodCallClientRequestHandler(srh);

        // Create the cave proxy, and login mikkel
        CaveProxy caveProxy = new CaveProxy(crh);
        Login loginResult = caveProxy.login("mikkel_aarskort", "123");

        player = (PlayerProxy) loginResult.getPlayer();
    }

    @Test
    public void shouldGetWeatherClientSide() {
        String weather = player.getWeather();

        assertThat(weather, containsString("The weather in ARHUS is Partly Cloudy, temperature 42.0C (feelslike 24.0C). Wind: 4.0 m/s, direction NNE."));
        assertThat(weather, containsString("This report is dated: Tue, 08 Sep 2015 13:24:22 +0200"));
    }

    @Test
    public void shouldGetErrorsClientSide() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        String weather = player.getWeather();
        assertThat(weather, containsString("The weather in ARHUS is Partly Cloudy, temperature 42.0C (feelslike 24.0C). Wind: 4.0 m/s, direction NNE."));
        assertThat(weather, containsString("This report is dated: Tue, 08 Sep 2015 13:24:22 +0200"));

        requestSaboteur.setThrowNext(new IOException());
        weather = player.getWeather();
        assertThat(weather, containsString("*** Sorry - weather information is not available ***"));

        weather = player.getWeather();
        assertThat(weather, containsString("The weather in ARHUS is Partly Cloudy, temperature 42.0C (feelslike 24.0C). Wind: 4.0 m/s, direction NNE."));
        assertThat(weather, containsString("This report is dated: Tue, 08 Sep 2015 13:24:22 +0200"));

        requestSaboteur.setThrowNext(new IOException());
        weather = player.getWeather();
        assertThat(weather, containsString("*** Sorry - weather information is not available ***"));

        requestSaboteur.setThrowNext(new IOException());
        weather = player.getWeather();
        assertThat(weather, containsString("*** Sorry - weather information is not available ***"));

        requestSaboteur.setThrowNext(new IOException());
        weather = player.getWeather();
        assertThat(weather, containsString("*** Sorry - weather information is not available ***"));


        requestSaboteur.setThrowNext(new IOException());
        weather = player.getWeather();
        assertThat(weather, containsString("*** Sorry - no weather (open circuit) ***"));
    }
}
