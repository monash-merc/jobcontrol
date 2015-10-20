package au.org.massive.strudel_web.vnc;

/**
 * Parameters requires for the Guacamole database
 *
 * @author jrigby
 */
public class GuacamoleSession {
    private static int instanceCount = 0;
    private int id;
    private String name;
    private String hostName;
    private int port;
    private String protocol;
    private String password;
    private GuacamoleUser user;

    public GuacamoleSession() {

    }

    public GuacamoleSession(int id, String name, String hostName, int port,
                            String protocol, String password, GuacamoleUser user) {
        super();
        instanceCount ++;
        this.id = instanceCount;
        this.name = name;
        this.hostName = hostName;
        this.port = port;
        this.protocol = protocol;
        this.password = password;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public GuacamoleUser getUser() {
        return user;
    }

    public void setUser(GuacamoleUser user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        return new Integer(id).equals(o);
    }

    @Override
    public int hashCode() {
        return new Integer(id).hashCode();
    }
}
