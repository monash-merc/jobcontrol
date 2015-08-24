package au.org.massive.strudel_web.gwt.client;

import au.org.massive.strudel_web.gwt.client.js_overlay_types.JobListObject;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.LinkedList;
import java.util.List;

/**
 * The state monitor polls continuously and notifies of a change of state
 * @param <T>
 */
public abstract class StateMonitor<T extends State> extends Timer {

    private static final int DEFAULT_POLL_RATE = 5000; // ms per poll
    private int pollRate;
    private List<StateChangeHandler<T>> stateChangeHandlers;

    public StateMonitor() {
        this(DEFAULT_POLL_RATE);
    }

    public StateMonitor(int pollRate) {
        super();
        this.pollRate = pollRate;
        stateChangeHandlers = new LinkedList<StateChangeHandler<T>>();
    }

    public void scheduleRepeating() {
        scheduleRepeating(pollRate);
    }

    protected void error(Throwable e) {
        if (e != null) {
            e.printStackTrace();
        }
    }

    public void registerStateChangeHandler(StateChangeHandler<T> stateChangeHandler) {
        stateChangeHandlers.add(stateChangeHandler);
    }

    protected void triggerStateChangeEvent(T oldState, T newState) {
        for (StateChangeHandler<T> stateChangeHandler : stateChangeHandlers) {
            stateChangeHandler.onStateChange(oldState, newState);
        }
    }
}
