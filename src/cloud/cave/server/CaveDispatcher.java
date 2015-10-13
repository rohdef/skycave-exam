package cloud.cave.server;

import org.json.simple.*;

import cloud.cave.domain.*;
import cloud.cave.ipc.*;

public class CaveDispatcher implements Dispatcher {
    private final Cave cave;

    public CaveDispatcher(final Cave cave) {
        this.cave = cave;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public JSONObject dispatch(final String methodKey, final String playerID, final String sessionID,
                               final String parameter1, final JSONArray parameterList) {
        final JSONObject reply;

        // === LOGIN
        if (methodKey.equals(MarshalingKeys.LOGIN_METHOD_KEY)) {
            final String loginName = parameter1;
            final String password = parameterList.get(0).toString();
            final Login result = cave.login(loginName, password);
            if (LoginResult.isValidLogin(result.getResultCode())) {
                final String playerId = result.getPlayer().getID();
                final String playerName = result.getPlayer().getName();
                final String sessionId = result.getPlayer().getSessionID();
                final String region = result.getPlayer().getRegion().toString();
                final String roomDescription = result.getPlayer().getShortRoomDescription();
                // add the player to the cache
                // cache.add(sessionId, result.getPlayer());
                reply = Marshaling.createValidReplyWithReturnValue(result.getResultCode().toString(), playerId,
                        playerName, sessionId, roomDescription);
                reply.put("player-region", region);
                reply.put("position", result.getPlayer().getPosition());
            } else {
                // The player id is set to "" as we do not know
                reply = Marshaling.createValidReplyWithReturnValue(result.getResultCode().toString());
            }
        }
        // === LOGOUT
        else if (methodKey.equals(MarshalingKeys.LOGOUT_METHOD_KEY)) {
            final LogoutResult result = cave.logout(playerID);

            // remove the player's session from the cache cache.remove(sessionID);

            reply = Marshaling.createValidReplyWithReturnValue(result.toString());
        }
        // === DESCRIBE CONFIGURATION
        else if (methodKey.equals(MarshalingKeys.DESCRIBE_CONFIGURATION_METHOD_KEY)) {
            reply = Marshaling.createValidReplyWithReturnValue(cave.describeConfiguration());
        } else {
            reply = null;
        }
        // No need for a 'default case' as the returned null value will be caught in the caller.

        return reply;
    }

}
