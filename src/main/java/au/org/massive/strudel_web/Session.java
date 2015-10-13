package au.org.massive.strudel_web;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import au.org.massive.strudel_web.ssh.CertAuthInfo;
import au.org.massive.strudel_web.vnc.GuacamoleSession;

/**
 * An abstraction from the {@link HttpSession} object, mediating access to session attributes
 *
 * @author jrigby
 */
public class Session {

    private final HttpSession session;

    public Session(String sessionId) throws NoSuchSessionException {
        session = SessionManager.getSessionById(sessionId);
        if (session == null) {
            throw new NoSuchSessionException();
        }
    }

    public Session(HttpSession session) {
        this.session = session;
    }

    public Session(HttpServletRequest request) {
        this(request.getSession());
    }

    public HttpSession getHttpSession() {
        return session;
    }

    public String getSessionId() {
        return session.getId();
    }

    // Session attribute keys
    private static final String USER_EMAIL = "user-email";
    private static final String KEY_CERT = "ssh-certificate";
    private static final String OAUTH_BACKEND = "oauth-backend";
    private static final String OAUTH_ACCESS_TOKEN = "oauth-access-token";
    private static final String GUAC_SESSION = "guacamole-session";

    public void setUserEmail(String email) {
        session.setAttribute(USER_EMAIL, email);
    }

    public String getUserEmail() {
        return (String) session.getAttribute(USER_EMAIL);
    }

    public boolean hasUserEmail() {
        return getUserEmail() != null;
    }

    public void setCertificate(CertAuthInfo cert) {
        session.setAttribute(KEY_CERT, cert);
    }

    public CertAuthInfo getCertificate() {
        return (CertAuthInfo) session.getAttribute(KEY_CERT);
    }

    public boolean hasCertificate() {
        return getCertificate() != null;
    }

    public void setSSHCertSigningBackend(SSHCertSigningBackend authBackend) {
        session.setAttribute(OAUTH_BACKEND, authBackend);
    }

    public SSHCertSigningBackend getSSHCertSigningBackend() {
        return (SSHCertSigningBackend) session.getAttribute(OAUTH_BACKEND);
    }

    public void setOAuthAccessToken(String accessToken) {
        session.setAttribute(OAUTH_ACCESS_TOKEN, accessToken);
    }

    public String getOAuthAccessToken() {
        return (String) session.getAttribute(OAUTH_ACCESS_TOKEN);
    }

    public boolean hasOAuthAccessToken() {
        return session.getAttribute(OAUTH_ACCESS_TOKEN) != null;
    }

    public void clearOAuthAccessToken() {
        session.removeAttribute(OAUTH_ACCESS_TOKEN);
    }

    @SuppressWarnings("unchecked")
    public synchronized Set<GuacamoleSession> getGuacamoleSessionsSet() {
        Set<GuacamoleSession> guacamoleSessions = (Set<GuacamoleSession>) session.getAttribute(GUAC_SESSION);
        if (session.getAttribute(GUAC_SESSION) == null) {
            guacamoleSessions = Collections.newSetFromMap(new ConcurrentHashMap<GuacamoleSession, Boolean>());
            session.setAttribute(GUAC_SESSION, guacamoleSessions);
        }

        // Handle multiple user sessions - return all Guacamole sessions belonging to the user even if over multiple sessions
        for (Session activeSession : SessionManager.getActiveSessions()) {
            if (!activeSession.getSessionId().equals(this.getSessionId()) && activeSession.hasCertificate() && this.hasCertificate() &&
                    activeSession.getCertificate().getUserName().equals(this.getCertificate().getUserName())) {
                Set<GuacamoleSession> otherGuacSessions = (Set<GuacamoleSession>) activeSession.getHttpSession().getAttribute(GUAC_SESSION);
                if (otherGuacSessions != null) {
                    guacamoleSessions.addAll(otherGuacSessions);
                    otherGuacSessions.clear();
                }
            }
        }
        return guacamoleSessions;
    }

    @Override
    public boolean equals(Object o) {
        return getSessionId().equals(o);
    }

    @Override
    public int hashCode() {
        return getSessionId().hashCode();
    }
}
