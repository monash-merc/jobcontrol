package au.org.massive.strudel_web.vnc;

import java.io.IOException;
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
 *
 * @author jrigby
 */
public class GuacamoleSessionManager implements ServletContextListener {

    private static final Settings settings = Settings.getInstance();

    private static Map<Integer, Tunnel> sshTunnels;
    private static Timer tunnelCleaner;

    public GuacamoleSessionManager() {
        sshTunnels = new HashMap<>();
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


    /**
     * Starts a Guacamole session
     * @param desktopName name to assign to desktop
     * @param vncPassword password to access VNC session
     * @param viaGateway the remote SSH server gateway
     * @param remoteHost the target of the tunnel
     * @param remotePort the remote port of the tunnel
     * @param session current session object
     * @return a GuacamoleSession with active tunnel
     */
    public static GuacamoleSession startSession(String desktopName, String vncPassword, String viaGateway, String remoteHost, int remotePort, Session session) {
        GuacamoleSession guacSession = new GuacamoleSession();
        try {
            guacSession.setName(desktopName);
            guacSession.setPassword(vncPassword);
            guacSession.setRemoteHost(remoteHost.equals("localhost")?viaGateway:remoteHost);
            guacSession.setProtocol("vnc");
            guacSession.setRemotePort(remotePort);

            // Avoid creating duplicate guacamole tunnels
            if (session.getGuacamoleSessionsSet().contains(guacSession)) {
                for (GuacamoleSession s : session.getGuacamoleSessionsSet()) {
                    if (s.equals(guacSession)) {
                        s.setName(desktopName);
                        s.setPassword(vncPassword);
                        return s;
                    }
                }
            } else {
                guacSession.setLocalPort(startTunnel(viaGateway, remoteHost, remotePort, session));
            }

            session.getGuacamoleSessionsSet().add(guacSession);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return guacSession;
    }

    public static void endSession(GuacamoleSession guacSession, Session session) {
        stopTunnel(guacSession.getLocalPort());
        session.getGuacamoleSessionsSet().remove(guacSession);
    }

    private static int startTunnel(String viaGateway, String remoteHost, int remotePort, Session session) throws IOException {
        ForkedSSHClient sshClient = new ForkedSSHClient(session.getCertificate(), viaGateway, remoteHost);
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
