package au.org.massive.strudel_web;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import au.org.massive.strudel_web.vnc.GuacamoleSession;
import au.org.massive.strudel_web.vnc.GuacamoleSessionManager;

/**
 * A session listener that cleans up any leftovers when a session ends, and keeps track of all active sessions
 * @author jrigby
 *
 */
public class SessionManager implements HttpSessionListener {
	
	private static Map<String,HttpSession> sessionMap = new HashMap<String,HttpSession>();
	
	public static HttpSession getSessionById(String id) {
		return sessionMap.get(id);
	}
	
	public static void endSession(String id) {
		HttpSession session = getSessionById(id);
		if (session != null) {
			session.invalidate();
		}
	}
	
	public static void endSession(Session session) {
		endSession(session.getHttpSession());
	}
	
	public static void endSession(HttpSession session) {
		session.invalidate();
	}
	
	public static Set<Session> getActiveSessions() {
		Set<Session> activeSessions = new HashSet<Session>();
		for (HttpSession s : sessionMap.values()) {
			activeSessions.add(new Session(s));
		}
		return activeSessions;
	}

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		sessionMap.put(event.getSession().getId(), event.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		Session s = new Session(event.getSession());
		
		// Clean up any left over guacamole sessions
		Set<GuacamoleSession> guacSessions = s.getGuacamoleSessionsSet();
		for (GuacamoleSession guacSession : guacSessions) {
			GuacamoleSessionManager.endSession(guacSession, s);
		}
		
		sessionMap.remove(event.getSession().getId());
	}

}
