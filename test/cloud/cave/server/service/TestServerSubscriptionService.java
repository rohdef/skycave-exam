package cloud.cave.server.service;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.domain.Region;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.server.common.SubscriptionRecord;
import cloud.cave.server.common.SubscriptionResult;
import cloud.cave.service.SubscriptionService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;


/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestServerSubscriptionService {
    private SubscriptionService subscriptionService;

    @Before
    public void setup() {
        CaveServerFactory factory = new AllTestDoubleFactory();
        subscriptionService = factory.createSubscriptionServiceConnector();
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
}
