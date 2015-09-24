package au.org.massive.strudel_web.gwt.client;

import au.org.massive.strudel_web.gwt.client.js_overlay_types.SessionStateObject;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by jason on 5/08/15.
 */
public class SessionManager extends StateMonitor<SessionState> {

    private static SessionManager instance;

    private SessionStateObject currentStateObject;
    private SessionState lastState;

    private static final String SESSION_STATE_URL = "api/session_info";
    private static final String CERT_SIGNING_URL = "api/register_key";
    private static final String LOGOUT_URL = "api/end_session";

    private static boolean sessionUpdateInProgress = false;

    private SessionManager() {
        super(10000); // poll every 10s
        lastState = SessionState.NOT_LOGGED_IN;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    @Override
    public void run() {
        refreshSession();
    }

    public void refreshSession() {
        refreshSession(null);
    }

    public void refreshSession(AsyncCallback<SessionState> callback) {
        if (!sessionUpdateInProgress) {
            sessionUpdateInProgress = true;
            triggerSessionRefresh(callback);
        }
    }

    private void triggerSessionRefresh() {
        triggerSessionRefresh(null);
    }

    private void triggerSessionRefresh(final AsyncCallback<SessionState> callback) {
        AjaxUtils.getData(SESSION_STATE_URL, new AsyncCallback<SessionStateObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                sessionUpdateInProgress = false;
                sessionRefreshError(throwable);
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(SessionStateObject javaScriptObject) {
                currentStateObject = javaScriptObject;
                currentState(); // Triggers events if necessary
                if (!currentStateObject.hasCertificate() && currentStateObject.hasOAuthAccessToken()) {
                    triggerCertificateSigningRequest();
                } else {
                    sessionUpdateInProgress = false;
                    if (callback != null) {
                        callback.onSuccess(currentState());
                    }
                }
            }
        });
    }

    private void triggerCertificateSigningRequest() {
        AjaxUtils.getData(CERT_SIGNING_URL, new AsyncCallback<JavaScriptObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                certSigningError(throwable);
            }

            @Override
            public void onSuccess(JavaScriptObject javaScriptObject) {
                triggerSessionRefresh();
            }
        });
    }

    public void invalidateSession() {
        AjaxUtils.getData(LOGOUT_URL, new AsyncCallback<JavaScriptObject>() {

            @Override
            public void onFailure(Throwable throwable) {
                logoutError(throwable);
            }

            @Override
            public void onSuccess(JavaScriptObject javaScriptObject) {
                triggerSessionRefresh();
            }
        });
    }

    public void loginKickoff() {
        loginPopup(currentStateObject.getSessionId());
    }

    private native void loginPopup(String sessionId) /*-{
	    $wnd.open("login?token="+sessionId, "_blank", "width=900, height=500");
    }-*/;

    private void logoutError(Throwable e) {
        StrudelWeb.log("Could not end session");
        error(e);
    }

    private void certSigningError(Throwable e) {
        StrudelWeb.log("Could not get SSH certificate signed");
        error(e);
    }

    private void sessionRefreshError(Throwable e) {
        StrudelWeb.log("Could not update session information");
        error(e);
    }

    public User currentUser() {
        return new User(this.currentStateObject.getUserName(), this.currentStateObject.getEmailAddress());
    }

    public SessionState currentState() {
        SessionState currentState;
        if (currentStateObject == null || !currentStateObject.hasOAuthAccessToken()) {
            currentState = SessionState.NOT_LOGGED_IN;
        } else if (currentStateObject.hasOAuthAccessToken() && !currentStateObject.hasCertificate()) {
            currentState = SessionState.LOGGED_IN_AWAITING_CERTIFICATE;
        } else {
            currentState = SessionState.LOGGED_IN;
        }

        if (currentState != lastState) {
            triggerStateChangeEvent(lastState, currentState);
            lastState = currentState;
        }

        return currentState;
    }
}
