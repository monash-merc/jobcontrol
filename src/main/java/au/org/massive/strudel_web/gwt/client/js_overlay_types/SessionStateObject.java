package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by jason on 5/08/15.
 */
public class SessionStateObject extends JavaScriptObject {

    protected SessionStateObject() {}

    public final native String getUserName() /*-{
        var defaultValue = "";
        if (!this) {return defaultValue}
        return this.uid || defaultValue;
    }-*/;

    public final native boolean hasCertificate() /*-{
        var defaultValue = false;
        if (!this) {return defaultValue}
        var r = this.has_certificate || defaultValue;
        return (typeof r === "boolean")?r:(r === "true");
    }-*/;

    public final native String getSessionId() /*-{
        var defaultValue = "";
        if (!this) {return defaultValue}
        return this.session_id || defaultValue;
    }-*/;

    public final native boolean hasOAuthAccessToken() /*-{
        var defaultValue = false;
        if (!this) {return defaultValue}
        var r = this.has_oauth_access_token || defaultValue;
        return (typeof r === "boolean")?r:(r === "true");
    }-*/;

    public final native String getEmailAddress() /*-{
        var defaultValue = "";
        if (!this) {return defaultValue}
        return this.email || defaultValue;
    }-*/;
}
