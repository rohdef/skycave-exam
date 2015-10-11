package cloud.cave.server.common;

import cloud.cave.domain.Region;

/**
 * This is a record type (struct / PODO (Plain Old Data Object)) representing
 * the core data of a player like name, id, position, etc.
 * <p/>
 * A record is a pure data object without any behavior, very suitable for
 * networking and persistence as it only contains data.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class PlayerRecord {
    private final String playerID;
    private final String playerName;
    private final String groupName;
    private final Region region;

    private String positionAsString;
    private final String sessionID;

    public PlayerRecord(String playerID, String playerName, String groupName, Region region, String positionAsString, String sessionID) {
        super();
        this.playerID = playerID;
        this.playerName = playerName;
        this.groupName = groupName;
        this.region = region;
        this.positionAsString = positionAsString;
        this.sessionID = sessionID;
    }

    public String getPlayerID() {
        return playerID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getPositionAsString() {
        return positionAsString;
    }

    public Region getRegion() {
        return region;
    }

    /**
     * get the session ID; if it is null
     * then the player is not presently in the cave
     *
     * @return the session ID or null in case
     * no session exists for the given player
     */
    public String getSessionId() {
        return sessionID;
    }

    public boolean isInCave() {
        return sessionID != null;
    }

    @Deprecated
    public void setPositionAsString(String positionAsString) {
        this.positionAsString = positionAsString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((playerID == null) ? 0 : playerID.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PlayerRecord other = (PlayerRecord) obj;
        if (playerID == null) {
            if (other.playerID != null)
                return false;
        } else if (!playerID.equals(other.playerID))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PlayerRecord [playerID=" + playerID + ", playerName=" + playerName
                + ", groupName=" + groupName + ", region=" + region
                + ", positionAsString=" + positionAsString + ", sessionID=" + sessionID
                + "]";
    }


}
