package au.org.massive.strudel_web.job_control;

/**
 * Represents a message that should be displayed to the user via the UI
 */
public class UserMessage {
    public enum MessageType {
        INFORMATION("info"), WARNING("warn"), ERROR("error");

        private String name;
        MessageType(String asString) {
            name = asString;
        }
        public String toString() {
            return name;
        }
    }

    private long timestamp;
    private MessageType type;
    private String message;
    public UserMessage(MessageType type, String message) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.message = message;
    }

    public MessageType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return getMessage();
    }

    public long getTimestamp() {
        return timestamp;
    }
}
