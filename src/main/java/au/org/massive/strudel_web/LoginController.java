package au.org.massive.strudel_web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.org.massive.strudel_web.job_control.ConfigurationRegistry;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

/**
 * The login controller kicks off the OAuth2 auth flow and notifies the client when auth is complete.
 * The view for the login controller is displayed in a popup window.
 */
public class LoginController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Settings settings = Settings.getInstance();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginController() {
        super();
    }

    /**
     * @throws IOException thrown on network IO errors
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Session session = new Session(request);

        ConfigurationRegistry systemConfigurations = settings.getSystemConfigurations();
        SSHCertSigningBackend authBackend;
        String authServiceId;
        if ((authServiceId = request.getParameter("service")) == null) {
            authServiceId = "default";
            authBackend = systemConfigurations.getDefaultSSHCertSigningBackend();
        } else {
            authBackend = systemConfigurations.getSSHCertSigningBackendById(authServiceId);
        }
        if (authBackend == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid authentication backend (typo in 'service' query string parameter?)");
            return;
        }

        session.setSSHCertSigningBackend(authBackend);

        // ** If no access token, go get one
        URI authRedirectUrl = URI.create(settings.getBaseUrl(request) + "?service=" + URLEncoder.encode(authServiceId, "utf-8"));
        if (!session.hasOAuthAccessToken()) {
            try {
                session.setCertificate(null);
                OAuthService.doAuthCodeRedirect(response, session, authRedirectUrl);
                return;
            } catch (OAuthSystemException e) {
                throw new RuntimeException(e);
            }
        }

        // ** Everything is fine; we have an oauth session
        // Send a very simple page to call the parent page's js loginComplete method.
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<title>JobControl Login</title>");
        out.println("</head>");
        out.println("<body>Login complete! You can close me now :)</body>");
        out.println("<script type=\"text/javascript\">");
        out.println("try { opener.loginComplete(); } catch (err) {}");
        out.println("window.close();");
        out.println("</script>");
        out.println("</html>");
        out.close();
    }
}
