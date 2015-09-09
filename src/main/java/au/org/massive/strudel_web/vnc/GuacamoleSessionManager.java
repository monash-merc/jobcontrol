package au.org.massive.strudel_web.vnc;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import au.org.massive.strudel_web.Session;
import au.org.massive.strudel_web.SessionManager;
import au.org.massive.strudel_web.Settings;
import au.org.massive.strudel_web.ssh.ForkedSSHClient;
import au.org.massive.strudel_web.ssh.Tunnel;

/**
 * Manages the lifecycle of a Guacamole session
 * @author jrigby
 *
 */
public class GuacamoleSessionManager implements ServletContextListener {
	
	private static final Settings settings = Settings.getInstance();
	
	private static Map<Integer,Tunnel> sshTunnels;
	private static Timer tunnelCleaner;
	
	public GuacamoleSessionManager() {
		sshTunnels = new HashMap<Integer,Tunnel>();
		tunnelCleaner = new Timer(true);
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		for (Tunnel t : sshTunnels.values()) {
			t.stopTunnel();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			GuacamoleDB.cleanDb();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		final long FIVE_SECONDS = 5000;
		tunnelCleaner.schedule(new TimerTask() {

			@Override
			public void run() {
				for (Tunnel t : sshTunnels.values()) {
					if (!t.isRunning()) {
						for (Session s : SessionManager.getActiveSessions()) {
							for (GuacamoleSession gs : s.getGuacamoleSessionsSet()) {
								GuacamoleSessionManager.endSession(gs, s);
							}
						}
					}
				}
			}
			
		}, FIVE_SECONDS, FIVE_SECONDS);
	}
	
	
	
	public static GuacamoleSession startSession(String desktopName, String vncPassword, String remoteHost, int remotePort, Session session) {
		GuacamoleSession guacSession = new GuacamoleSession();
		try {
			guacSession.setName(desktopName);
			guacSession.setPassword(vncPassword);
			guacSession.setHostName(settings.getGuacdHost());
			guacSession.setProtocol("vnc");
			guacSession.setPort(startTunnel(remoteHost, remotePort, session));
			guacSession.setUser(new GuacamoleUser(0, session.getCertificate().getUserName(), session.getUserEmail()));
			GuacamoleDB.createSession(guacSession);
			if (guacSession.getId() > -1) { // A session of -1 indicates failure (i.e. attempt at creating a duplicate tunnel
											// so don't add it to the set.
				session.getGuacamoleSessionsSet().add(guacSession);
			}
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
		return guacSession;
	}
	
	public static void endSession(GuacamoleSession guacSession, Session session) {
		stopTunnel(guacSession.getPort());
		try {
			GuacamoleDB.deleteSession(guacSession);
			session.getGuacamoleSessionsSet().remove(guacSession);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static int startTunnel(String remoteHost, int remotePort, Session session) throws IOException {
		ForkedSSHClient sshClient = new ForkedSSHClient(session.getCertificate(), remoteHost);
		Tunnel t = sshClient.startTunnel(remotePort, 0);
		sshTunnels.put(t.getLocalPort(), t);
		return t.getLocalPort();
	}
	
	private static boolean stopTunnel(int guacdPort) {
		if (sshTunnels.containsKey(guacdPort)) {
			Tunnel t = sshTunnels.get(guacdPort);
			if (t.isRunning()) {
				t.stopTunnel();
			}
			sshTunnels.remove(guacdPort);
			return true;
		} else {
			return false;
		}
	}
}
