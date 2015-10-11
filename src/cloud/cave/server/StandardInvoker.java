package cloud.cave.server;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import org.json.simple.*;
import org.slf4j.*;

import cloud.cave.domain.*;
import cloud.cave.ipc.*;

/**
 * Standard implementation of the Invoker.
 *
 * @author Henrik Baerbak Christensen, University of Aarhus
 */
public class StandardInvoker implements Invoker {
    private static final Logger logger = LoggerFactory.getLogger(StandardInvoker.class);
    private final Map<String, Dispatcher> mapKey2Dispatch;

    /**
     * Create an invoker that dispatches requests using default dispatching, that is the
     * set of methods known in the initial release of SkyCave.
     *
     * @param cave the cave that all requests manipulate
     */
    public StandardInvoker(Cave cave) {
        // Create the map that maps from class prefix to dispatcher for that particular class type,
        // see Reactor pattern and 'identifyDispather' method.
        Map<String, Dispatcher> mapKey2Dispatch = new HashMap<>();
        mapKey2Dispatch.put(MarshalingKeys.CAVE_TYPE_PREFIX, new CaveDispatcher(cave));
        mapKey2Dispatch.put(MarshalingKeys.PLAYER_TYPE_PREFIX, new PlayerDispatcher(cave));

        this.mapKey2Dispatch = ImmutableMap.copyOf(mapKey2Dispatch);
    }

    /**
     * Create an invoker that dispatches requests to the given cave using a given dispatcher.
     *
     * @param cave                       the cave that all requests manipulate
     * @param mapTypePrefixToDispatchers a Map that maps strings to dispatchers; the key string must
     *                                   be one of the type prefixes, like "player-", defined in the MarshalingKeys.
     */
    public StandardInvoker(Cave cave, Map<String, Dispatcher> mapTypePrefixToDispatchers) {
        mapKey2Dispatch = ImmutableMap.copyOf(mapTypePrefixToDispatchers);
    }


    @Override
    public JSONObject handleRequest(final JSONObject request) {
        final JSONObject reply, dispatcherResponse;

        // Extract the common parameters from the request object and assign
        // them names that reflect their meaning
        final String playerID = request.get(MarshalingKeys.PLAYER_ID_KEY).toString();
        final String sessionID = request.get(MarshalingKeys.PLAYER_SESSION_ID_KEY).toString();
        final String methodKey = request.get(MarshalingKeys.METHOD_KEY).toString();
        final String parameter1;
        final Object parameter1AsObj = request.get(MarshalingKeys.PARAMETER_HEAD_KEY);
        if (parameter1AsObj != null) {
            parameter1 = parameter1AsObj.toString();
        } else {
            parameter1 = "";
        }
        final JSONArray parameterList = (JSONArray) request.get(MarshalingKeys.PARAMETER_TAIL_KEY);

        // Dispatch the event (POSA vol 4 Reactor code)
        final Dispatcher dispatcher = identifyDispatcher(methodKey);

        // We may get a null object back if the method key is ill formed  thus guard the dispatch call
        if (dispatcher != null) {
            // Next, do the dispatching - based upon the parameters, call  the proper method on the proper object
            dispatcherResponse = dispatcher.dispatch(methodKey, playerID, sessionID, parameter1, parameterList);

            if (dispatcherResponse != null) {
                reply = dispatcherResponse;
            } else {
                // UNHANDLED METHOD
                reply = Marshaling.createInvalidReplyWithExplantion(StatusCode.SERVER_UNKNOWN_METHOD_FAILURE,
                        "StandardInvoker.handleRequest: Unhandled request as the method key " + methodKey
                                + " is unknown. Full request=" + request.toString());
                logger.warn("\u001b[1;31mhandleRequest: Unhandled request as the method key " + methodKey
                        + " is unknown. Full request=" + request.toString() + "\u001b[0;37m");
            }
        } else {
            // UNHANDLED METHOD
            reply = Marshaling.createInvalidReplyWithExplantion(StatusCode.SERVER_UNKNOWN_METHOD_FAILURE,
                    "StandardInvoker.handleRequest: Unknown dispatcher for the methodKey: " + methodKey
                            + ". Full request=" + request.toString());
            logger.warn("\u001b[1;31Unknown dispatcher for the methodKey: " + methodKey
                    + " is unknown. Full request=" + request.toString() + "\u001b[0;37m");
        }

        return reply;
    }

    /**
     * Identify the dispatcher appropriate for the given method. Corresponds to the identify_handler(event) in Reactor
     * pattern.
     * <p/>
     * Presently relies on mangled method names, i.e. that a method on the player starts with 'play' and a method
     * on cave starts with 'cave'.
     *
     * @param methodKey key of the method, see MarshalingKeys
     * @return the appropriate dispatcher for the class containing that particular method or null if the method key
     * is ill-formed
     */
    private Dispatcher identifyDispatcher(final String methodKey) {
        final int firstDash = methodKey.indexOf("-");
        final String key = methodKey.substring(0, firstDash + 1);
        return mapKey2Dispatch.get(key);
    }
}
