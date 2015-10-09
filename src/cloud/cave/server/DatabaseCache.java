package cloud.cave.server;

import cloud.cave.domain.Player;
import cloud.cave.server.service.ServerCaveStorage;
import cloud.cave.service.CaveStorage;
import cloud.cave.service.WeatherService;
import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class DatabaseCache implements PlayerSessionCache {
    private final CaveStorage storage;
    private final WeatherService weatherService;

    public DatabaseCache(CaveStorage storage, WeatherService weatherService) {
        this.storage = storage;
        this.weatherService = weatherService;
    }

    @Override
    public Player get(String playerID) {
        StandardServerPlayer player = new StandardServerPlayer(playerID, storage, weatherService, this);
        if (Strings.isNullOrEmpty(player.getSessionID()))
            return null;
        else
            return player;
    }

    @Override
    public void add(String playerID, Player player) {
    }

    @Override
    public void remove(String playerID) {
    }
}
