package au.org.massive.strudel_web.gwt.client;

import au.org.massive.strudel_web.gwt.client.js_overlay_types.JobObject;
import au.org.massive.strudel_web.gwt.client.js_overlay_types.VncTunnelObject;

import java.util.HashMap;

/**
 * Created by jrigby on 10/08/2015.
 */
public class JobTunnelMap extends HashMap<JobObject, VncTunnelObject> {

    public boolean hasTunnel(JobObject job) {
        return hasTunnel(Integer.valueOf(job.getJobId()));
    }

    public boolean hasTunnel(int jobId) {
        for (JobObject j : this.keySet()) {
            if (Integer.valueOf(j.getJobId()) == jobId) {
                return this.get(j) != null;
            }
        }
        return false;
    }
}
