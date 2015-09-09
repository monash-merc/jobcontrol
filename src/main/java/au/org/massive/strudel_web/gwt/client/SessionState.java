package au.org.massive.strudel_web.gwt.client;

/**
 * Created by jason on 5/08/15.
 */
public enum SessionState implements State {
    NOT_LOGGED_IN("NOT_LOGGED_IN"),
    LOGGED_IN_AWAITING_CERTIFICATE("LOGGED_IN_AWAITING_CERTIFICATE"),
    LOGGED_IN("LOGGED_IN");

    private final String stringValue;
    SessionState(String stringValue) {
        this.stringValue = stringValue;
    }

    public String toString() {
        return stringValue;
    }
}
