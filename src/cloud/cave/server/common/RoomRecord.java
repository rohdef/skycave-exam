package cloud.cave.server.common;

import java.util.List;

/**
 * This is a record type (struct / PODO (Plain Old Data Object)) representing
 * the core data of a room. At present there is not much more to a room than a
 * textual description.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class RoomRecord {

    public String description;
    private List<String> messageList;

    public RoomRecord(String description, List<String> messageList) {
        this.description = description;
        this.messageList = messageList;
    }

    public List<String> getMessageList() {
        return messageList;
    }

    public void addMessage(String message) {
        messageList.add(message);
    }
}