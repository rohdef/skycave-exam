package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.doubles.RequestFake;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.WeatherService;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestServerWeatherService {
    private WeatherService weatherService;
    private String group, user, host;
    private int port;
    private RequestFake fakeRequest;
    private ServerConfiguration serverConfiguration;

    @Before
    public void setup() {
        host = "is-there-anybody-out-there";
        port = 57005;
        group = "TheFooBars";
        user = "WhiskyMonster";
        fakeRequest = new RequestFake();
        serverConfiguration = new ServerConfiguration(host, port);
        weatherService = new ServerWeatherService(fakeRequest);
        weatherService.initialize(serverConfiguration);
    }

    @Test
    public void shouldReturnWeather() {
        JSONObject weather = weatherService.requestWeather(group, user, Region.ARHUS);

        assertTrue(weather.containsKey("errorMessage"));
        assertTrue(weather.containsKey("windspeed"));
        assertTrue(weather.containsKey("weather"));
        assertTrue(weather.containsKey("feelslike"));
        assertTrue(weather.containsKey("temperature"));
    }

    @Test
    public void shouldAccountForTheCityChosen() {
        JSONObject weather = weatherService.requestWeather(group, user, Region.ARHUS);
        assertThat(weather.get("temperature").toString(), containsString("42.0"));
        assertThat(weather.get("windspeed").toString(), containsString("4"));

        weather = weatherService.requestWeather(group, user, Region.COPENHAGEN);
        assertThat(weather.get("temperature").toString(), containsString("-14.0"));
        assertThat(weather.get("windspeed").toString(), containsString("25"));

        weather = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(weather.get("temperature").toString(), containsString("15.0"));
        assertThat(weather.get("windspeed").toString(), containsString("3"));

        weather = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(weather.get("temperature").toString(), containsString("21.0"));
        assertThat(weather.get("windspeed").toString(), containsString("15"));
    }

    @Test
    public void shouldProduceErrorWhenNoConfigurationIsPresent() {
        // Note this test relies on that RequestFake will cause errors on invalid urls
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.requestWeather(group, user, Region.ARHUS);
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertTrue(true);
        }

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(null);
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
            assertThat(e.getMessage(), containsString("The ServerConfiguration must be set"));
        }
    }

    @Test
    public void shouldProduceErrorWhenInvalidConfigurationIsUsed() {
        // Null values are not valid
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(new ServerConfiguration(null, null));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        // Java does not accept new ServerConfiguration("", null), bummer would have been nice to break it here 3:-)

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(new ServerConfiguration(null, 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // Empty string is not a valid server
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("", 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // 0 or below is not a valid port
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("abc", 0));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new RequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("goodbye", -42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }
    }

    @Test
    public void shouldProduceErrorWhenTheCallDataIsInvalid() {
        try {
            weatherService.requestWeather(null, user, Region.ODENSE);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("group"));
        }

        try {
            weatherService.requestWeather("", user, Region.ODENSE);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("group"));
        }

        try {
            weatherService.requestWeather(group, null, Region.ODENSE);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("user"));
        }

        try {
            weatherService.requestWeather(group, "", Region.ODENSE);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("user"));
        }

        try {
            weatherService.requestWeather(group, user, null);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
            assertThat(e.getMessage(), containsString("region"));
        }

        try {
            weatherService.requestWeather(null, null, null);
            assertTrue("Expected an error with wrong parameter specifications", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            // group is the first parameter
            assertThat(e.getMessage(), containsString("group"));
        }
    }

    @Test
    public void shouldCallWithAValidUrl() {
        weatherService.requestWeather(group, user, Region.AALBORG);
        String url = fakeRequest.getLastUrl();
        if (url.substring(0, 7).equalsIgnoreCase("http://"))
            url = url.substring(7);

        String[] urlParts = url.split("/");
        assertThat(urlParts.length, is(8));
        assertThat(urlParts[0], containsString(host+":"+port));
        assertThat(urlParts[1], containsString("cave"));
        assertThat(urlParts[2], containsString("weather"));
        assertThat(urlParts[3], containsString("api"));
        assertThat(urlParts[4], containsString("v1"));
        assertThat(urlParts[5], containsString(group));
        assertThat(urlParts[6], containsString(user));
        assertThat(urlParts[7], containsString("Aalborg"));

        weatherService.requestWeather(group, user, Region.ODENSE);
        url = fakeRequest.getLastUrl();
        if (url.substring(0, 7).equalsIgnoreCase("http://"))
            url = url.substring(7);

        urlParts = url.split("/");
        assertThat(urlParts.length, is(8));
        assertThat(urlParts[7], containsString("Odense"));
    }

    @Test
    public void shouldNotCrashOnGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(fakeRequest);
        weatherService = new ServerWeatherService(requestSaboteur);
        weatherService.initialize(serverConfiguration);

        try {
            requestSaboteur.setGarbage("");
            weatherService.requestWeather(group, user, Region.ODENSE);
            assertTrue("Expected an error on garbage input", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getCause(), instanceOf(ParseException.class));
            assertThat(e.getMessage(), containsString("JSON"));
        }

        try {
            requestSaboteur.setGarbage("{\"errorMessage\":\"OK\"," +
                    "\"windspeed\":\"4\"," +
                    "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                    "\"authenticated\":\"true\"," +
                    "\"weather\":\"Partly Cloudy\"," +
                    "\"winddirection\":\"NNE\"," +
                    "\"feelslike\":\"24.0\"," +
                    "\"temperature\":\"42.0\"}HAHA");
            weatherService.requestWeather(group, user, Region.ODENSE);
            assertTrue("Expected an error on garbage input", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getCause(), instanceOf(ParseException.class));
            assertThat(e.getMessage(), containsString("JSON"));
        }

        try {
            requestSaboteur.setGarbage("hejsa");
            weatherService.requestWeather(group, user, Region.ODENSE);
            assertTrue("Expected an error on garbage input", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getCause(), instanceOf(ParseException.class));
            assertThat(e.getMessage(), containsString("JSON"));
        }
    }

    @Test
    public void shouldHandleRequestExceptions() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(fakeRequest);
        weatherService = new ServerWeatherService(requestSaboteur);
        weatherService.initialize(serverConfiguration);

        try {
            requestSaboteur.setThrowNext(new ClientProtocolException("HTTP does not stand for Hot Topless Teen Porn ><"));
            weatherService.requestWeather(group, user, Region.AALBORG);
            assertTrue("Expected an error", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getCause(), instanceOf(ClientProtocolException.class));
            assertThat(e.getMessage().toLowerCase(), containsString("connection"));
            assertThat(e.getMessage().toLowerCase(), containsString("rest"));
            assertThat(e.getMessage().toLowerCase(), containsString("service"));
        }

        try {
            requestSaboteur.setThrowNext(new IOException("IO is a moon you fool"));
            weatherService.requestWeather(group, user, Region.ODENSE);
            assertTrue("Expected an error", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getCause(), instanceOf(IOException.class));
            assertThat(e.getMessage().toLowerCase(), containsString("connection"));
            assertThat(e.getMessage().toLowerCase(), containsString("rest"));
            assertThat(e.getMessage().toLowerCase(), containsString("service"));
        }

        try {
            requestSaboteur.setThrowNext(new IllegalArgumentException("I'd like to have an argument, please!"));
            weatherService.requestWeather(group, user, Region.AALBORG);
            assertTrue("Expected an error", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }
}
