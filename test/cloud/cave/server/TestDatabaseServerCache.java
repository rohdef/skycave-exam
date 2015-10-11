package cloud.cave.server;

import cloud.cave.domain.Region;
import cloud.cave.doubles.AllTestDoubleFactory;
import cloud.cave.server.common.PlayerRecord;
import cloud.cave.service.CaveStorage;
import cloud.cave.service.WeatherService;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestDatabaseServerCache {
    private DatabaseCache cache;

    @Before
    public void setup() {
        AllTestDoubleFactory factory = new AllTestDoubleFactory();
        CaveStorage storage = factory.createCaveStorage();
        storage.updatePlayerRecord(new PlayerRecord("missInformation", "Miss Information", "BX5", Region.ODENSE, "(0,0,0)", null));
        storage.updatePlayerRecord(new PlayerRecord("misterInformation", "Mister Information", "BX5", Region.ODENSE, "(0,0,0)", "here"));

        cache = new DatabaseCache(storage, null);
    }

    @Test
    public void shouldGiveNullOnInvalidSessionId() {
        assertThat(cache.get("missInformation"), is(nullValue()));
        assertThat(cache.get("misterInformation"), is(notNullValue()));
    }

    @Test
    public void shouldCallRemoveWithoutErrors() {
        cache.remove("missInformation");
        assertThat(true, is(true));
    }
}
