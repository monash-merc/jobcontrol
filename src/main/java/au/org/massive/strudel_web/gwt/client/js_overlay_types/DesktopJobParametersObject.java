package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by jrigby on 24/08/2015.
 */
public class DesktopJobParametersObject extends JavaScriptObject implements DesktopJobParameters {
    protected DesktopJobParametersObject() {

    }

    @Override
    public native final int getHours() /*-{
        return this.hours;
    }-*/;

    @Override
    public native final int getMinutes() /*-{
        return this.minutes;
    }-*/;

    @Override
    public native final int getNodes() /*-{
        return this.nodes;
    }-*/;

    @Override
    public native final int getPpn() /*-{
        return this.ppn;
    }-*/;

    @Override
    public native final String getQueue() /*-{
        return this.queue;
    }-*/;
}
