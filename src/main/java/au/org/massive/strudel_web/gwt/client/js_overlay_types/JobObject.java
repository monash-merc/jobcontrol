package au.org.massive.strudel_web.gwt.client.js_overlay_types;

import com.google.gwt.core.client.JavaScriptObject;

public class JobObject extends JavaScriptObject {

    protected JobObject() {

    }

    public final native String getJobId() /*-{
        return this.jobId;
    }-*/;

    public final native String getJobName() /*-{
        return this.jobName;
    }-*/;

    public final native String getState() /*-{
        return this.state;
    }-*/;

    public final native String getRemainingWalltime() /*-{
        return this.remainingWalltime;
    }-*/;

}
