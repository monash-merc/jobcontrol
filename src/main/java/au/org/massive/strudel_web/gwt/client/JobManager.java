package au.org.massive.strudel_web.gwt.client;


import au.org.massive.strudel_web.gwt.client.js_overlay_types.*;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.LinkedList;
import java.util.List;

public class JobManager extends StateMonitor<JobListObject> {

    private static JobManager instance;

    private static final String LIST_JOBS_URL="api/execute/listjobs";
    private static final String LIST_TUNNELS_URL="api/listvnctunnels";
    private static final String START_TUNNEL_URL="api/startvnctunnel";
    private static final String DESTROY_TUNNEL_URL="api/stopvnctunnel";
    private static final String EXEC_HOST_URL="api/execute/exechost";
    private static final String VNC_DISPLAY_URL="api/execute/vncdisplay";
    private static final String VNC_PASSWORD_URL="api/execute/vncpassword";
    private static final String START_DESKTOP_URL="api/execute/startdesktop";
    private static final String STOP_DESKTOP_URL="api/execute/stopjob";
    private static final String GET_VNC_PASSWORD_URL="api/execute/vncpassword";
    private static final String UPDATE_VNC_TUNNEL_PASSWORD="api/updatevncpwd";

    private boolean jobListUpdateInProgress = false;

    private JobListObject lastJobListObject = null;
    private JobListObject currentJobListObject = null;

    private JobTunnelMap currentJobs;

    private JobManager() {
        super(3000); // poll every 3s
        currentJobs = new JobTunnelMap();
    }

    public static JobManager getInstance() {
        if (instance == null) {
            instance = new JobManager();
        }

        return instance;
    }

    @Override
    public void run() {
        refreshJobList();
    }

    @Override
    public void cancel() {
        super.cancel();
        currentJobListObject = null;
        lastJobListObject = null;
    }

    public void refreshJobList() {
        refreshJobList(null);
    }

    public void refreshJobList(final AsyncCallback<JobListObject> callback) {
        if (!jobListUpdateInProgress) {
            jobListUpdateInProgress = true;
            getUpdatedJobAndTunnelList(new AsyncCallback<JobTunnelMap>() {
                @Override
                public void onFailure(Throwable throwable) {
                    jobListUpdateInProgress = false;
                    if (callback != null) {
                        callback.onFailure(throwable);
                    }
                    error(throwable);
                }

                @Override
                public void onSuccess(JobTunnelMap newJobMap) {
                    jobListUpdateInProgress = false;

                    if (currentJobListObject == null && lastJobListObject != null) {
                        triggerStateChangeEvent(lastJobListObject, lastJobListObject);
                    } else if (currentJobListObject.length() != lastJobListObject.length()) {
                        triggerStateChangeEvent(currentJobListObject, lastJobListObject);
                    } else {
                        for (int i = 0; i < currentJobListObject.length(); i++) {
                            JobObject jobA = currentJobListObject.get(i);
                            JobObject jobB = lastJobListObject.get(i);
                            if (jobA.getJobName() != jobB.getJobName() ||
                                    jobA.getJobId() != jobB.getJobId() ||
                                    jobA.getState() != jobB.getState()) {
                                triggerStateChangeEvent(currentJobListObject, lastJobListObject);
                            }
                        }
                    }

                    if (callback != null) {
                        callback.onSuccess(lastJobListObject);
                    }

                    currentJobListObject = lastJobListObject;
                    currentJobs = newJobMap;
                }
            });
        } else if (callback != null) {
            callback.onSuccess(currentJobListObject);
        }
    }

    private void getUpdatedJobAndTunnelList(final AsyncCallback<JobTunnelMap> callback) {

        final List<JobObject> currentJobs = new LinkedList<JobObject>();

        final AsyncCallback<VncTunnelListObject> processJobsAndTunnels = new AsyncCallback<VncTunnelListObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(VncTunnelListObject vncTunnelListObject) {
                final JobTunnelMap jobsAndTunnels = new JobTunnelMap();
                LinkedList<JobObject> tunnelsToBringUp = new LinkedList<JobObject>();
                for (JobObject j : currentJobs) {
                    boolean tunnelFound = false;
                    for (int i = 0; i < vncTunnelListObject.length(); i++) {
                        VncTunnelObject v = vncTunnelListObject.get(i);
                        if (v.getDesktopName().equals(j.getJobId()+"-"+j.getJobName())) {
                            jobsAndTunnels.put(j, v);
                            tunnelFound = true;
                            break;
                        }
                    }
                    if (!tunnelFound) {
                        // Skip non-desktop jobs
                        if (j.getJobName().startsWith("desktop")) {
                            tunnelsToBringUp.add(j);
                        }
                        jobsAndTunnels.put(j, null);
                    }
                }
                if (tunnelsToBringUp.size() > 0) {
                    bringUpTunnels(tunnelsToBringUp);
                }

                callback.onSuccess(jobsAndTunnels);
            }
        };

        getJobList(new AsyncCallback<JobListObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(JobListObject jobListObject) {
                for (int i = 0; i < jobListObject.length(); i++) {
                    currentJobs.add(jobListObject.get(i));
                }
                lastJobListObject = jobListObject;
                getTunnelList(processJobsAndTunnels);
            }
        });
    }

    private void bringUpTunnels(List<JobObject> jobs) {
        for (final JobObject j : jobs) {
            final VncTunnelCredentials tunnelCredentials = new VncTunnelCredentials();
            final Command setupTunnelIfReady = new Command() {
                @Override
                public void execute() {
                    if (tunnelCredentials.isValid()) {
                        String url = START_TUNNEL_URL+"?desktopname="+j.getJobId()+"-"+j.getJobName()
                                +"&display="+tunnelCredentials.getDisplay()
                                +"&remotehost="+tunnelCredentials.getExecHost()
                                +"&vncpassword="+tunnelCredentials.getPassword();
                        AjaxUtils.<JavaScriptObject>getData(url, new AsyncCallback<JavaScriptObject>() {
                            @Override
                            public void onFailure(Throwable throwable) {
                                error(throwable);
                            }

                            @Override
                            public void onSuccess(JavaScriptObject javaScriptObject) {
                                // do nothing
                            }
                        });
                    }
                }
            };

            AjaxUtils.<ExecHostObject>getData(EXEC_HOST_URL+"?jobid="+j.getJobId(), new AsyncCallback<ExecHostObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    error(throwable);
                }

                @Override
                public void onSuccess(ExecHostObject execHostObject) {
                    tunnelCredentials.setExecHost(execHostObject.getExecHost());
                    setupTunnelIfReady.execute();
                }
            });

            AjaxUtils.<VncDisplayObject>getData(VNC_DISPLAY_URL+"?jobid="+j.getJobId(), new AsyncCallback<VncDisplayObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    error(throwable);
                }

                @Override
                public void onSuccess(VncDisplayObject vncDisplayObject) {
                    tunnelCredentials.setDisplay(vncDisplayObject.getVncDisplay());
                    setupTunnelIfReady.execute();
                }
            });

            AjaxUtils.<VncPasswordObject>getData(VNC_PASSWORD_URL+"?jobid="+j.getJobId(), new AsyncCallback<VncPasswordObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    error(throwable);
                }

                @Override
                public void onSuccess(VncPasswordObject vncPasswordObject) {
                    tunnelCredentials.setPassword(vncPasswordObject.getPassword());
                    setupTunnelIfReady.execute();
                }
            });
        }
    }

    private void bringDownTunnels(List<VncTunnelObject> tunnels) {
        for (VncTunnelObject t : tunnels) {
            AjaxUtils.getData(DESTROY_TUNNEL_URL + "?id=" + t.getTunnelId(), new AsyncCallback<JavaScriptObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    error(throwable);
                }

                @Override
                public void onSuccess(JavaScriptObject javaScriptObject) {
                    // Do nothing
                }
            });
        }
    }

    private void getJobList(final AsyncCallback<JobListObject> callback) {
        AjaxUtils.<JobListObject>getData(LIST_JOBS_URL, callback);
    }

    private void getTunnelList(final AsyncCallback<VncTunnelListObject> callback) {
        AjaxUtils.<VncTunnelListObject>getData(LIST_TUNNELS_URL, callback);
    }

    public JobListObject getCurrentJobList() {
        return lastJobListObject;
    }

    public JobTunnelMap getJobTunnelMap() {
        return currentJobs;
    }

    public void startJob(DesktopJobParameters params, AsyncCallback<JavaScriptObject> callback) {
        StrudelWeb.log("Attempting to start desktop: "
                +params.getHours()+":"+params.getMinutes()
                +" nodes="+params.getNodes()+", ppn="+params.getPpn()+", queue="+params.getQueue());

        String urlParams = "?hours="+params.getHours()
                +"&minutes="+params.getMinutes()
                +"&nodes="+params.getNodes()
                +"&ppn="+params.getPpn()
                +"&queue="+ URL.encodeQueryString(params.getQueue());
        AjaxUtils.<JavaScriptObject>getData(START_DESKTOP_URL+urlParams, callback);
    }

    public void stopJob(int jobId, AsyncCallback<JavaScriptObject> callback) {
        String urlParams = "?jobid="+jobId;
        AjaxUtils.<JavaScriptObject>getData(STOP_DESKTOP_URL+urlParams, callback);
    }

    public void getVncPassword(int jobId, AsyncCallback<VncPasswordObject> callback) {
        String urlParams = "?jobid="+jobId;
        AjaxUtils.<VncPasswordObject>getData(GET_VNC_PASSWORD_URL+urlParams, callback);
    }

    public void updateVncTunnelPassword(String desktopName, String password, AsyncCallback<JavaScriptObject> callback) {
        String urlParams = "?desktopname="+URL.encodeQueryString(desktopName)
                +"&vncpassword="+URL.encodeQueryString(password);
        AjaxUtils.<JavaScriptObject>getData(UPDATE_VNC_TUNNEL_PASSWORD+urlParams, callback);
    }

}
