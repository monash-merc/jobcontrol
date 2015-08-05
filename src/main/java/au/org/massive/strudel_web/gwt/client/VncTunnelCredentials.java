package au.org.massive.strudel_web.gwt.client;

/**
 * Created by jrigby on 10/08/2015.
 */
public class VncTunnelCredentials {
    private String execHost;
    private String password;
    private String display;

    public VncTunnelCredentials() {
        this("", "", "");
    }

    public VncTunnelCredentials(String execHost, String password, String display) {
        this.execHost = execHost;
        this.password = password;
        this.display = display;
    }

    public boolean isValid() {
        return !execHost.isEmpty() && !password.isEmpty() && !display.isEmpty();
    }

    public String getExecHost() {
        return execHost;
    }

    public void setExecHost(String execHost) {
        this.execHost = execHost;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }
}
