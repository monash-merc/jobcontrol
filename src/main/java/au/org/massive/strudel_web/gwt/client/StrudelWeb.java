package au.org.massive.strudel_web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by jason on 5/08/15.
 */
public class StrudelWeb implements EntryPoint {

    @Override
    public void onModuleLoad() {
        // Periodically updates the user's session information
        final SessionManager sessionManager = SessionManager.getInstance();
        final JobManager jobManager = JobManager.getInstance();

        Exports.publish();

        sessionManager.registerStateChangeHandler(new StateChangeHandler<SessionState>() {
            @Override
            public void onStateChange(SessionState oldState, SessionState newState) {
                switch (newState) {
                    case LOGGED_IN:
                        sessionManager.scheduleRepeating();
                        jobManager.scheduleRepeating();
                        break;
                    case NOT_LOGGED_IN:
                        sessionManager.cancel();
                        jobManager.cancel();
                        break;
                }
            }
        });

        sessionManager.refreshSession(new AsyncCallback<SessionState>() {
            @Override
            public void onFailure(Throwable throwable) {
                Window.alert(throwable.getMessage());
            }

            @Override
            public void onSuccess(SessionState sessionState) {
                if (sessionManager.currentState() == SessionState.LOGGED_IN) {
                    sessionManager.scheduleRepeating();
                    jobManager.scheduleRepeating();
                }
                appReady();
            }
        });
    }

    private native void appReady() /*-{
        console.log("StrudelCore bootstrap complete.");
        if (typeof $wnd.appReady === 'function') {
            $wnd.appReady();
        }
    }-*/;

    public static native void log(String msg) /*-{
        console.log(msg);
    }-*/;
}
