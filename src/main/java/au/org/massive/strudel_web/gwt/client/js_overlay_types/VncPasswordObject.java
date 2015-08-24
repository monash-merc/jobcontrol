package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by jrigby on 7/08/2015.
 */
public class VncPasswordObject extends JavaScriptObject {
    protected VncPasswordObject() {

    }

    public native final String getPassword() /*-{
        if (this.length !== 1) {
            return "";
        }
        return this[0].password;
    }-*/;
}
