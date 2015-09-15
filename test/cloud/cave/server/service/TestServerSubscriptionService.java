package cloud.cave.server.service;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.domain.Region;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.doubles.SubscriptionServiceRequestFake;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.common.SubscriptionRecord;
import cloud.cave.server.common.SubscriptionResult;
import cloud.cave.service.SubscriptionService;
import org.apache.http.client.ClientProtocolException;
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
public class TestServerSubscriptionService {
    private SubscriptionService subscriptionService;
    private SubscriptionServiceRequestFake fakeRequest;

    @Before
    public void setup() {
        CaveServerFactory factory = new AllTestDoubleFactory();
        subscriptionService = factory.createSubscriptionServiceConnector();
        fakeRequest = new SubscriptionServiceRequestFake();
        subscriptionService.setRestRequester(fakeRequest);
    }

    @Test
    public void shouldBeAbleToLogTestUsersIn() {
        String user, password;
        SubscriptionRecord subscriptionRecord;

        user = "mikkel_aarskort";
        password = "123";
        subscriptionRecord = subscriptionService.lookup(user, password);

        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
        assertThat(subscriptionRecord.getPlayerID(), is("user-001"));
        assertThat(subscriptionRecord.getPlayerName(), is("Mikkel"));
        assertThat(subscriptionRecord.getGroupName(), is("grp01"));
        assertThat(subscriptionRecord.getRegion(), is(Region.ARHUS));


        user = "magnus_aarskort";
        password = "312";
        subscriptionRecord = subscriptionService.lookup(user, password);

        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
        assertThat(subscriptionRecord.getPlayerID(), is("user-002"));
        assertThat(subscriptionRecord.getPlayerName(), is("Magnus"));
        assertThat(subscriptionRecord.getGroupName(), is("grp01"));
        assertThat(subscriptionRecord.getRegion(), is(Region.COPENHAGEN));


        user = "mathilde_aarskort";
        password = "321";
        subscriptionRecord = subscriptionService.lookup(user, password);

        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
        assertThat(subscriptionRecord.getPlayerID(), is("user-003"));
        assertThat(subscriptionRecord.getPlayerName(), is("Mathilde"));
        assertThat(subscriptionRecord.getGroupName(), is("grp02"));
        assertThat(subscriptionRecord.getRegion(), is(Region.AALBORG));


        user = "reserved_aarskort";
        password = "cloudarch";
        subscriptionRecord = subscriptionService.lookup(user, password);

        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
        assertThat(subscriptionRecord.getPlayerID(), is("user-reserved"));
        assertThat(subscriptionRecord.getPlayerName(), is("ReservedCrunchUser"));
        assertThat(subscriptionRecord.getGroupName(), is("zzz0"));
        assertThat(subscriptionRecord.getRegion(), is(Region.ARHUS));
    }

    @Test
    public void shouldRejectUnknownUsers() {
        String user, password;
        SubscriptionRecord subscriptionRecord;

        user = "mikkel_aarskort";
        password = "321";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));

        user = "maleficient";
        password = "apple";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));

        user = "Robert'); DROP TABLE students;--";
        password = "Little Bobby Tables";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shouldProduceErrorWhenNoConfigurationIsPresent() {
        String user, password;
        SubscriptionRecord subscriptionRecord;
        user = "mikkel_aarskort";
        password = "123";

        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();
        try {
            subscriptionService.initialize(null);
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
            assertThat(e.getMessage(), containsString("The ServerConfiguration must be set"));
        }
    }

    @Test
    public void shouldProduceErrorWhenInvalidConfigurationIsUsed() {
        // Null values are not valid
        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();

        try {
            subscriptionService.initialize(new ServerConfiguration(null, null));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        // Java does not accept new ServerConfiguration("", null), bummer would have been nice to break it here 3:-)

        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();

        try {
            subscriptionService.initialize(new ServerConfiguration(null, 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // Empty string is not a valid server
        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();

        try {
            subscriptionService.initialize(new ServerConfiguration("", 42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The host must be set"));
        }

        // 0 or below is not a valid port
        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();

        try {
            subscriptionService.initialize(new ServerConfiguration("abc", 0));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }

        subscriptionService.disconnect();
        subscriptionService = new ServerSubscriptionService();

        try {
            subscriptionService.initialize(new ServerConfiguration("goodbye", -42));
            assertTrue("Exception is expected since this results in an invalid url", false);
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
            assertThat(e.getMessage(), containsString("The port must be set"));
        }
    }

    @Test
    public void shoudlProduceErrorWhenTheUserIsNull() {
        String password;
        SubscriptionRecord subscriptionRecord;

        password = "123";
        subscriptionRecord = subscriptionService.lookup(null, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shoudlProduceErrorWhenTheUserIsEmpty() {
        String user, password;
        SubscriptionRecord subscriptionRecord;

        user = "";
        password = "123";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shoudlProduceErrorWhenThePasswordIsNull() {
        String user;
        SubscriptionRecord subscriptionRecord;

        user = "mikkel_aarskort";
        subscriptionRecord = subscriptionService.lookup(user, null);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shoudlProduceErrorWhenThePasswordIsEmpty() {
        String user, password;
        SubscriptionRecord subscriptionRecord;

        user = "mikkel_aarskort";
        password = "";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shoudlProduceErrorWhenAlleAreNull() {
        SubscriptionRecord subscriptionRecord;

        subscriptionRecord = subscriptionService.lookup(null, null);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shoudlProduceErrorWhenAlleAreEmpty() {
        String user, password;
        SubscriptionRecord subscriptionRecord;

        user = "";
        password = "";
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shouldCallWithAValidUrl() {
        String user, password;
        user = "foo";
        password = "bar";

        subscriptionService.lookup(user, password);
        String url = fakeRequest.getLastUrl();
        if (url.substring(0, 7).equalsIgnoreCase("http://"))
            url = url.substring(7);

        String[] urlParts = url.split("/");
        assertThat(urlParts.length, is(4));
        assertThat(urlParts[0], containsString("is-there-anybody-out-there:57005"));
        assertThat(urlParts[1], containsString("api"));
        assertThat(urlParts[2], containsString("v1"));
        assertThat(urlParts[3], containsString("auth"));

        String[] lastUrlSplit = urlParts[3].split("[?]");
        assertThat(lastUrlSplit.length, is(2));

        String[] queryParams = lastUrlSplit[1].split("[&]");
        assertThat(queryParams.length, is(2));
        assertThat(queryParams[0], containsString(user));
        assertThat(queryParams[1], containsString(password));
    }

    @Test
    public void shouldNotCrashOnEmptyGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("");
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }


    @Test
    public void shouldNotCrashOnNullGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.activateNullGarbage();
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void shouldNotCrashOnMalformedJsonGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("{\"success\":true," +
                "\"subscription\":" +
                "{\"groupName\":\"CSS 25\"," +
                "\"dateCreated\":\"2015-08-31 13:06 PM UTC\"," +
                "\"playerName\":\"rohdef\"," +
                "\"loginName\":\"20052356\"," +
                "\"region\":\"AARHUS\"," +
                "\"playerID\":\"55e45169e4b067dd3c8fa56e\"}," +
                "\"message\":\"loginName 20052356 was authenticated\"}HAHA");
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void shouldNotCrashOnWeatherGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("{\"errorMessage\":\"OK\"," +
                "\"windspeed\":\"4\"," +
                "\"time\":\"Tue, 08 Sep 2015 13:24:22 +0200\"," +
                "\"authenticated\":\"true\"," +
                "\"weather\":\"Partly Cloudy\"," +
                "\"winddirection\":\"NNE\"," +
                "\"feelslike\":\"24.0\"," +
                "\"temperature\":\"42.0\"}");
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_OR_PASSWORD_IS_UNKNOWN));
    }

    @Test
    public void shouldNotCrashOnNotJsonAtAllGarbageResult() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setGarbage("Rush - Subdivisions");
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void shouldHandleClientProtocolException() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new ClientProtocolException("HTTP does not stand for Hot Topless Teen Porn ><"));
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void shouldHandleRequestIOException() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new IOException("IO is a moon you fool"));
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void souldHandleUnexpectedError() {
        RequestSaboteur requestSaboteur = new RequestSaboteur(fakeRequest);
        subscriptionService.setRestRequester(requestSaboteur);

        requestSaboteur.setThrowNext(new IllegalArgumentException("I'd like to have an argument, please!"));
        String user = "mikkel_aarskort";
        String password = "123";
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test(expected = NullPointerException.class)
    public void shouldGiveErrorWhenSettingTheRestRequesterToNull() {
        SubscriptionService subscriptionService = new ServerSubscriptionService();
        subscriptionService.setRestRequester(null);
    }

    @Test
    public void shouldGiveTheCurrentConfig() {
        assertThat(subscriptionService.getConfiguration(), is(not(nullValue())));

        subscriptionService = new ServerSubscriptionService();
        assertThat(subscriptionService.getConfiguration(), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptZeroSecondsDelay() {
        subscriptionService.setSecondsDelay(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptMinusOneSecondsDelay() {
        subscriptionService.setSecondsDelay(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptRandomNegativeSecondsDelay() {
        subscriptionService.setSecondsDelay(-56587);
    }
}
