package au.org.massive.strudel_web.vnc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import au.org.massive.strudel_web.Settings;

/**
 * Data access methods for the Guacamole session database
 *
 * @author jrigby
 */
public class GuacamoleDB {
    private static final Settings settings = Settings.getInstance();

    // JDBC driver name and database URL
    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://" + settings.getGuacMySQLHost() + ":" + settings.getGuacMySQLPort() + "/" + settings.getGuacMySQLDBName();
    private static final String USER = settings.getGuacMySQLUserName();
    private static final String PASSWORD = settings.getGuacMySQLPassword();

    private GuacamoleDB() {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    public static void addGuacUserIfMissing(GuacamoleUser user) throws SQLException {
        GuacamoleDB db = new GuacamoleDB();

        PreparedStatement query;
        try (Connection conn = db.getConnection()) {
            // Check if email exists
            query = conn.prepareStatement("SELECT * FROM vnc_user WHERE email=? LIMIT 1");
            query.setString(1, user.getEmail());
            ResultSet result = query.executeQuery();
            // If the user exists, update the user object and return it
            if (result.first()) {
                user.setId(result.getInt("id"));
                user.setName(result.getString("name"));
                return;
            } else {
                // Otherwise, create the user
                query = conn.prepareStatement("INSERT INTO vnc_user (name, email) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
                query.setString(1, user.getName());
                query.setString(2, user.getEmail());
                query.executeUpdate();
                ResultSet keysResultSet = query.getGeneratedKeys();
                if (keysResultSet.next()) {
                    user.setId(keysResultSet.getInt(1));
                }
                return;
            }
        }
    }

    public static void cleanDb() throws SQLException {
        GuacamoleDB db = new GuacamoleDB();
        PreparedStatement query;
        Connection conn = db.getConnection();
        query = conn.prepareStatement("TRUNCATE TABLE vnc_connection");
        query.execute();
    }

    public static void createSession(GuacamoleSession session) throws SQLException {
        // Add the guacamole user if it's missing
        addGuacUserIfMissing(session.getUser());

        GuacamoleDB db = new GuacamoleDB();
        PreparedStatement query;
        try (Connection conn = db.getConnection()) {
            query = conn.prepareStatement("SELECT COUNT(*) FROM vnc_connection WHERE name=?");
            query.setString(1, session.getName());
            ResultSet r = query.executeQuery();
            if (r.first() && r.getInt(1) == 0) {
                query = conn.prepareStatement("INSERT INTO vnc_connection (name, host_name, port, protocol, password, user_id) values (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                query.setString(1, session.getName());
                query.setString(2, session.getHostName());
                query.setInt(3, session.getPort());
                query.setString(4, session.getProtocol());
                query.setString(5, session.getPassword());
                query.setInt(6, session.getUser().getId());
                query.executeUpdate();
                ResultSet keysResultSet = query.getGeneratedKeys();
                if (keysResultSet.next()) {
                    session.setId(keysResultSet.getInt(1));
                }
            } else {
                // Duplicate desktop name attempted. ID of -1 indicates failure
                session.setId(-1);
            }
        }
    }

    public static void updateSession(GuacamoleSession session) throws SQLException {
        // Add the guacamole user if it's missing
        addGuacUserIfMissing(session.getUser());

        GuacamoleDB db = new GuacamoleDB();
        PreparedStatement query;
        try (Connection conn = db.getConnection()) {
            query = conn.prepareStatement("UPDATE vnc_connection SET name=?, host_name=?, port=?, protocol=?, password=?, user_id=? WHERE id=?", Statement.RETURN_GENERATED_KEYS);
            query.setString(1, session.getName());
            query.setString(2, session.getHostName());
            query.setInt(3, session.getPort());
            query.setString(4, session.getProtocol());
            query.setString(5, session.getPassword());
            query.setInt(6, session.getUser().getId());
            query.setInt(7, session.getId());
            query.executeUpdate();
        }
    }

    public static Set<GuacamoleSession> getAllSessions() throws SQLException {
        Set<GuacamoleSession> sessions = new HashSet<>();
        GuacamoleDB db = new GuacamoleDB();
        PreparedStatement query;
        Connection conn = db.getConnection();

        query = conn.prepareStatement("SELECT vnc_connection.id as conn_id,"
                + "vnc_connection.name as conn_name,"
                + "vnc_connection.host_name as host_name,"
                + "vnc_connection.port as port,"
                + "vnc_connection.protocol as protocol,"
                + "vnc_connection.password as password,"
                + "vnc_user.id as user_id,"
                + "vnc_user.name as user_name,"
                + "vnc_user.email as email FROM vnc_connection, vnc_user WHERE vnc_connection.user_id = vnc_user.id");
        query.execute();
        ResultSet rs = query.getResultSet();
        while (rs.next()) {
            GuacamoleSession session = new GuacamoleSession();
            session.setId(rs.getInt("conn_id"));
            session.setName(rs.getString("conn_name"));
            session.setHostName(rs.getString("host_name"));
            session.setPort(rs.getInt("port"));
            session.setProtocol(rs.getString("protocol"));
            session.setPassword(rs.getString("password"));
            session.setUser(new GuacamoleUser(rs.getInt("user_id"), rs.getString("user_name"), rs.getString("email")));
            sessions.add(session);
        }
        return sessions;
    }

    public static Set<GuacamoleSession> deleteSessionsOtherThan(Set<GuacamoleSession> sessions) throws SQLException {
        Set<GuacamoleSession> registeredSessions = getAllSessions();
        registeredSessions.removeAll(sessions);
        deleteSessions(registeredSessions);
        return registeredSessions;
    }

    public static void deleteSessions(Collection<GuacamoleSession> sessions) throws SQLException {
        for (GuacamoleSession s : sessions) {
            deleteSession(s);
        }
    }

    public static void deleteSession(GuacamoleSession session) throws SQLException {
        deleteSession(session.getId());
    }

    public static void deleteSession(int sessionId) throws SQLException {
        GuacamoleDB db = new GuacamoleDB();
        PreparedStatement query;

        try (Connection conn = db.getConnection()) {
            query = conn.prepareStatement("DELETE FROM vnc_connection WHERE id = ?");
            query.setInt(1, sessionId);
            query.execute();
        }
    }

}
