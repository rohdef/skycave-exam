package cloud.cave.server.service;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.domain.Region;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.doubles.WeatherServiceRequestFake;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.WeatherService;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
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
    private String group, user;
    private WeatherServiceRequestFake fakeRequest;

    @Before
    public void setup() {
        group = "grp01";
        user = "WhiskyMonster";

        CaveServerFactory factory = new AllTestDoubleFactory();
        weatherService = factory.createWeatherServiceConnector();
        fakeRequest = new WeatherServiceRequestFake();
        weatherService.setRestRequester(fakeRequest);
    }

    @Test
    public void shouldReturnWeather() {
        JSONObject weather = weatherService.requestWeather(group, user, Region.AARHUS);

        assertTrue(weather.containsKey("errorMessage"));
        assertTrue(weather.containsKey("windspeed"));
        assertTrue(weather.containsKey("weather"));
        assertTrue(weather.containsKey("feelslike"));
        assertTrue(weather.containsKey("temperature"));
    }

    @Test
    public void shouldAccountForTheCityChosen() {
        JSONObject weather = weatherService.requestWeather(group, user, Region.AARHUS);
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
        // Note this test relies on that WeatherServiceRequestFake will cause errors on invalid urls
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AARHUS);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(null);
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
            assertThat(e.getMessage(), containsString("The ServerConfiguration must be set"));
        }

        try {
            weatherService.disconnect();
            weatherService = new ServerWeatherService(null);
            assertTrue("Exception is expected since the requester is null", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
            assertThat(e.getMessage(), containsString("The rest request must be set"));
        }
    }

    @Test
    public void shouldProduceErrorWhenInvalidConfigurationIsUsed() {
        // Null values are not valid
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(new ServerConfiguration(null, null));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        // Java does not accept new ServerConfiguration("", null), bummer would have been nice to break it here 3:-)

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(new ServerConfiguration(null, 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // Empty string is not a valid server
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("", 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // 0 or below is not a valid port
        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("abc", 0));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }

        weatherService.disconnect();
        weatherService = new ServerWeatherService(new WeatherServiceRequestFake());

        try {
            weatherService.initialize(new ServerConfiguration("goodbye", -42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }
    }

    @Test
    public void shouldProduceErrorWhenTheGroupIsNull() {
        JSONObject jsonObject = weatherService.requestWeather(null, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldProduceErrorWhenTheGroupIsEmpty() {
        JSONObject jsonObject = weatherService.requestWeather("", user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldProduceErrorWhenUserIsNull() {
        JSONObject jsonObject = weatherService.requestWeather(group, null, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldProduceErrorWhenUserIsBlank() {
        JSONObject jsonObject = weatherService.requestWeather(group, "", Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));}

    @Test
    public void shouldProduceErrorWhenRegionIsNull() {
        JSONObject jsonObject = weatherService.requestWeather(group, user, null);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldProduceErrorWhenCalledWithAllNulls() {
        JSONObject jsonObject = weatherService.requestWeather(null, null, null);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldCallWithAValidUrl() {
        weatherService.requestWeather(group, user, Region.AALBORG);
        String url = fakeRequest.getLastUrl();
        if (url.substring(0, 7).equalsIgnoreCase("http://"))
            url = url.substring(7);

        String[] urlParts = url.split("/");
        assertThat(urlParts.length, is(8));
        assertThat(urlParts[0], containsString("is-there-anybody-out-there:57005"));
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
    public void shouldNotCrashOnEmptyGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("");
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldNotCrashOnNullGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.activateNullGarbage();
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldNotCrashOnMalformedJsonGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("{\"errorMessage\":\"OK\"," +
                "\"windspeed\":\"4\"," +
                "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                "\"authenticated\":\"true\"," +
                "\"weather\":\"Partly Cloudy\"," +
                "\"winddirection\":\"NNE\"," +
                "\"feelslike\":\"24.0\"," +
                "\"temperature\":\"42.0\"}HAHA");
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldNotCrashOnRegistrationJsonGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("{\"success\":true," +
                "\"subscription\":" +
                "{\"groupName\":\"CSS 25\"," +
                "\"dateCreated\":\"2015-08-31 13:06 PM UTC\"," +
                "\"playerName\":\"rohdef\"," +
                "\"loginName\":\"20052356\"," +
                "\"region\":\"AARHUS\"," +
                "\"playerID\":\"55e45169e4b067dd3c8fa56e\"}," +
                "\"message\":\"loginName 20052356 was authenticated\"}");
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldNotCrashOnNotJsonAtAllGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("hejsa");
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldHandleClientProtocolException() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new ClientProtocolException("HTTP does not stand for Hot Topless Teen Porn ><"));
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldHandleRequestIOException() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(weatherService.getRestRequester());
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new IOException("IO is a moon you fool"));
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.ODENSE);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void souldHandleUnexpectedError() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(fakeRequest);
        weatherService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new IllegalArgumentException("I'd like to have an argument, please!"));
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test(expected = NullPointerException.class)
    public void shouldGiveErrorWhenSettingTheRestRequesterToNull() {
        WeatherService weatherService = new ServerWeatherService();
        weatherService.setRestRequester(null);
    }

    @Test
    public void shouldGiveTheCurrentConfig() {
        assertThat(weatherService.getConfiguration(), is(not(nullValue())));

        weatherService = new ServerWeatherService();
        assertThat(weatherService.getConfiguration(), is(nullValue()));
    }
}
