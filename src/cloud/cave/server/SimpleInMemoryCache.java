package cloud.cave.server;

import java.util.*;

import cloud.cave.domain.Player;

/**
 * Implementation of the player session cache using a Map data structure.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class SimpleInMemoryCache implements PlayerSessionCache {
    private Map<String, Player> cacheOfOnlinePlayer;

    public SimpleInMemoryCache() {
        cacheOfOnlinePlayer = new HashMap<>();
    }

    @Override
    public Player get(String sessionID) {
        return cacheOfOnlinePlayer.get(sessionID);
    }

    @Override
    public void add(String sessionID, Player player) {
        cacheOfOnlinePlayer.put(sessionID, player);
    }

    @Override
    public void remove(String sessionID) {
        cacheOfOnlinePlayer.remove(sessionID);
    }
}
