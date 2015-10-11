package cloud.cave.server.common;


import cloud.cave.domain.Region;

/**
 * Record type / PODO representing the subscription for a player. This
 * encapsulates the basic data that is transferred from the SubscriptionService
 * for authenticating a player.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class SubscriptionRecord {
    private final String playerName;
    private final String playerID;
    private final String groupName;
    private final Region region;

    private SubscriptionResult errorCode;

    /**
     * Construct a subscription match result that failed validation.
     *
     * @param errorCode the code of the error describing the failed subscription
     */
    public SubscriptionRecord(SubscriptionResult errorCode) {
        this(null, null, null, null, errorCode);
    }

    /**
     * Construct a valid subscription object.
     *
     * @param playerID   id of player
     * @param playerName name of player
     * @param groupName  name of group
     * @param region     region of player
     */
    public SubscriptionRecord(String playerID, String playerName, String groupName, Region region) {
        this(playerName, playerID, groupName, region, SubscriptionResult.LOGIN_NAME_HAS_VALID_SUBSCRIPTION);
    }

    private SubscriptionRecord(String playerName, String playerID, String groupName, Region region, SubscriptionResult errorCode) {
        this.playerName = playerName;
        this.playerID = playerID;
        this.groupName = groupName;
        this.region = region;
        this.errorCode = errorCode;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPlayerID() {
        return playerID;
    }

    public Region getRegion() {
        return region;
    }

    public SubscriptionResult getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "SubscriptionRecord [playerName=" + playerName + ", playerID=" + playerID + ", groupName=" + groupName
                + ", region=" + region + ", errorCode=" + errorCode + "]";
    }

    public String getGroupName() {
        return groupName;
    }

}
