package au.org.massive.strudel_web.vnc;

/**
 * Represents a user entry in the Guacamole database
 *
 * @author jrigby
 */
public class GuacamoleUser {
    private int id;
    private String name;
    private String email;

    public GuacamoleUser() {

    }

    public GuacamoleUser(int id, String name, String email) {
        super();
        this.id = id;
        this.name = name;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
