package au.org.massive.strudel_web.gwt.client;

import au.org.massive.strudel_web.gwt.client.js_overlay_types.DesktopJobParametersObject;
import au.org.massive.strudel_web.gwt.client.js_overlay_types.JobListObject;
import au.org.massive.strudel_web.gwt.client.js_overlay_types.VncPasswordObject;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Created by jason on 5/08/15.
 */
public abstract class Exports {

    private static class NativeAsyncCallback<T extends JavaScriptObject> implements AsyncCallback<T> {

        private JavaScriptObject jsCallbackSuccess, jsCallbackFailure;

        public NativeAsyncCallback(JavaScriptObject jsCallbackSuccess, JavaScriptObject jsCallbackFailure) {
            this.jsCallbackSuccess = jsCallbackSuccess;
            this.jsCallbackFailure = jsCallbackFailure;
        }

        @Override
        public void onFailure(Throwable throwable) {
            executeCallback(jsCallbackFailure, throwable.toString());
        }

        @Override
        public void onSuccess(T t) {
            executeCallback(jsCallbackSuccess, t);
        }

        // ***** Callback helper methods *****
        private static native void executeCallback(JavaScriptObject callback, JavaScriptObject data) /*-{
            if (typeof callback === 'function') {
                callback(data);
            }
        }-*/;

        private static native void executeCallback(JavaScriptObject callback, String msg) /*-{
            if (typeof callback === 'function') {
                callback(msg);
            }
        }-*/;
    }

    // ***** Session methods *****
    public static abstract class Session {
        public static String loginStatus() {
            return SessionManager.getInstance().currentState().toString();
        }

        public static void triggerLogin() {
            SessionManager.getInstance().loginKickoff();
        }

        public static void triggerLogout() {
            SessionManager.getInstance().invalidateSession();
        }

        public static void refreshSession() {
            SessionManager.getInstance().refreshSession();
        }

        public static void registerSessionStateChangeEvent(final JavaScriptObject event) {
            StateChangeHandler<SessionState> handler = new StateChangeHandler<SessionState>() {
                @Override
                public void onStateChange(SessionState oldState, SessionState newState) {
                    stateChange(event, oldState == null ? "" : oldState.toString(), newState == null ? "" : newState.toString());
                }

                private native void stateChange(JavaScriptObject event, String oldState, String newState) /*-{
                    if (typeof event === "function") {
                        event(oldState, newState);
                    }
                }-*/;
            };
            SessionManager.getInstance().registerStateChangeHandler(handler);
        }

        public static User currentUser() {
            return SessionManager.getInstance().currentUser();
        }

        public static void startSessionPolling() {
            SessionManager.getInstance().scheduleRepeating();
        }
    }

    // ***** Task control methods *****
    public static abstract class JobControl {
        public static JobListObject getJobList() {
            return JobManager.getInstance().getCurrentJobList();
        }

        public static void registerJobStateChangeEvent(final JavaScriptObject event) {
            StateChangeHandler<JobListObject> handler = new StateChangeHandler<JobListObject>() {
                @Override
                public void onStateChange(JobListObject oldState, JobListObject newState) {
                    stateChange(event, oldState, newState);
                }

                private native void stateChange(JavaScriptObject event, JobListObject oldState, JobListObject newState) /*-{
                    if (typeof event === "function") {
                        event(oldState, newState);
                    }
                }-*/;
            };
            JobManager.getInstance().registerStateChangeHandler(handler);
        }

        public static boolean jobHasTunnel(int jobId) {
            return JobManager.getInstance().getJobTunnelMap().hasTunnel(jobId);
        }

        public static void refreshJobList(JavaScriptObject callbackSuccess, JavaScriptObject callbackFailure) {
            JobManager.getInstance().refreshJobList(new NativeAsyncCallback<JobListObject>(callbackSuccess, callbackFailure));
        }

        public static void startDesktop(DesktopJobParametersObject params, JavaScriptObject callbackSuccess, JavaScriptObject callbackFailure) {
            JobManager.getInstance().startJob(params, new NativeAsyncCallback<JavaScriptObject>(callbackSuccess, callbackFailure));
        }

        public static void stopDesktop(int jobId, JavaScriptObject callbackSuccess, JavaScriptObject callbackFailure) {
            JobManager.getInstance().stopJob(jobId, new NativeAsyncCallback<JavaScriptObject>(callbackSuccess, callbackFailure));
        }

        public static void getVncPassword(int jobId, final JavaScriptObject callbackSuccess, final JavaScriptObject callbackFailure) {
            JobManager.getInstance().getVncPassword(jobId, new AsyncCallback<VncPasswordObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    executeCallback(callbackFailure, throwable.toString());
                }

                @Override
                public void onSuccess(VncPasswordObject vncPasswordObject) {
                    executeCallback(callbackSuccess, vncPasswordObject.getPassword());
                }

                private native void executeCallback(JavaScriptObject callback, String result) /*-{
                    if (typeof callback === 'function') {
                        callback(result);
                    }
                }-*/;
            });
        }

        public static void updateVncTunnelPassword(String desktopName, String password, JavaScriptObject callbackSuccess, JavaScriptObject callbackFailure) {
            JobManager.getInstance().updateVncTunnelPassword(desktopName, password, new NativeAsyncCallback<JavaScriptObject>(callbackSuccess, callbackFailure));
        }
    }

    public static native void publish() /*-{
        $wnd.__strudel = {
            session: {
                refresh: @au.org.massive.strudel_web.gwt.client.Exports.Session::refreshSession(),
                loginStatus: @au.org.massive.strudel_web.gwt.client.Exports.Session::loginStatus(),
                currentUser: @au.org.massive.strudel_web.gwt.client.Exports.Session::currentUser(),
                triggerLogin: @au.org.massive.strudel_web.gwt.client.Exports.Session::triggerLogin(),
                triggerLogout: @au.org.massive.strudel_web.gwt.client.Exports.Session::triggerLogout(),
                registerStateChangeEvent: @au.org.massive.strudel_web.gwt.client.Exports.Session::registerSessionStateChangeEvent(*)
            },
            jobControl: {
                startDesktop: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::startDesktop(*),
                stopDesktop: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::stopDesktop(*),
                getJobList: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::getJobList(),
                getVncPassword: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::getVncPassword(*),
                updateVncTunnelPassword: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::updateVncTunnelPassword(*),
                refreshJobList: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::refreshJobList(*),
                jobHasTunnel: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::jobHasTunnel(*),
                registerStateChangeEvent: @au.org.massive.strudel_web.gwt.client.Exports.JobControl::registerJobStateChangeEvent(*)
            }
        };

        $wnd.loginComplete = function() {
            $wnd.__strudel.session.refresh();
            @au.org.massive.strudel_web.gwt.client.Exports.Session::startSessionPolling()();
        };
    }-*/;
}
