package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by jrigby on 7/08/2015.
 */
public class VncTunnelObject extends JavaScriptObject {
    protected VncTunnelObject() {

    }

    public native final int getTunnelId() /*-{
        return this.id;
    }-*/;

    public native final String getDesktopName() /*-{
        return this.desktopName;
    }-*/;

    public native final String getVncPassword() /*-{
        return this.password;
    }-*/;
}
