package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by jrigby on 7/08/2015.
 */
public class ExecHostObject extends JavaScriptObject {
    protected ExecHostObject() {

    }

    public native final String getExecHost() /*-{
        if (this.length !== 1) {
            return "";
        }
        return this[0].execHost;
    }-*/;
}
