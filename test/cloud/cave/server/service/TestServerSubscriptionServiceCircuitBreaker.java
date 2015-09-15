package cloud.cave.server.service;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.server.common.SubscriptionRecord;
import cloud.cave.server.common.SubscriptionResult;
import cloud.cave.service.SubscriptionService;
import org.apache.http.client.ClientProtocolException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.MissingResourceException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestServerSubscriptionServiceCircuitBreaker {
    private SubscriptionService subscriptionService;
    private String user, password;
    private int port;
    private RequestSaboteur fakeRequest;
    private ServerConfiguration serverConfiguration;

    @Before
    public void setup() {
        CaveServerFactory factory = new AllTestDoubleFactory();
        subscriptionService = factory.createSubscriptionServiceConnector();
        fakeRequest = new RequestSaboteur(subscriptionService.getRestRequester());
        subscriptionService.setRestRequester(fakeRequest);

        user = "mikkel_aarskort";
        password = "123";
    }

    @Test
    public void shouldGiveUnavailbleErrorWhenUnvailableService() {
        fakeRequest.setThrowNext(new IOException("Could not connect to server"));

        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));
    }

    @Test
    public void shouldGiveUnavailbleErrorOnUnexpectetError() {
        fakeRequest.setThrowNext(new MissingResourceException("Yes, sir. ... Oh, I'm sorry sir, I thought you were referring to me, Mr Wensleydale.",
                "Moon.class",
                "getCheese()"));

        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

    }

    @Test
    public void shouldGiveUnavailbleErrorWhenGarbageIsReturned() {
        fakeRequest.setGarbage("Tanja loeber hurtigt");

        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

    }

    @Test
    public void shouldOpenAfterThreeAttempts() {
        fakeRequest.setThrowNext(new ClientProtocolException());
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));
    }

    @Test
    public void shouldHalfOpenAfterOneSecond() throws InterruptedException {
        subscriptionService.setSecondsDelay(1);
        fakeRequest.setThrowNext(new ClientProtocolException());
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setThrowNext(new RuntimeException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(800);

        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(200);
        fakeRequest.reset();
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
    }

    @Test
    public void shouldCacheLoggedInUsers() throws InterruptedException {
        subscriptionService.setSecondsDelay(1);
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));

        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));

        String user2 = "mathilde_aarskort";
        String password2 = "321";

        subscriptionRecord = subscriptionService.lookup(user2, password2);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(800);

        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));

        subscriptionRecord = subscriptionService.lookup(user2, password2);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(200);
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));

        subscriptionRecord = subscriptionService.lookup(user2, password2);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION));
    }

    @Test
    public void shouldHalfOpenAfterOneSecondWithError() throws InterruptedException {
        subscriptionService.setSecondsDelay(1);
        fakeRequest.setThrowNext(new ClientProtocolException());
        SubscriptionRecord subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_CLOSED));


        fakeRequest.setThrowNext(new RuntimeException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(800);

        fakeRequest.setThrowNext(new RuntimeException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));

        Thread.sleep(200);
        fakeRequest.setThrowNext(new IOException());
        subscriptionRecord = subscriptionService.lookup(user, password);
        assertThat(subscriptionRecord.getErrorCode(), is(SubscriptionResult.LOGIN_SERVICE_UNAVAILABLE_OPEN));
    }
}
