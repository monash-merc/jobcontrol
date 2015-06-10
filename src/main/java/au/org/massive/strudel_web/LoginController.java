package au.org.massive.strudel_web;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

/**
 * The login controller kicks off the OAuth2 auth flow and notifies the client when auth is complete.
 * The view for the login controller is displayed in a popup window.
 */
public class LoginController extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LoginController() {
        super();
    }

	/**
	 * @throws IOException 
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Session session;
		try {
			session = new Session(request.getParameter("token"));
		} catch (NoSuchSessionException e) { 
			// If no session exists
			String message = "A session ID must be sent with the login request. "
					+ "This may be obtained from the /api/session_info endpoint and included as a \"token\" query string parameter.";
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
			return;
		}
		
		// ** If no access token and no key pair exists, go get one
		URI authRedirectUrl = URI.create(request.getRequestURL().toString()+"?token="+session.getSessionId());
		if (!session.hasOAuthAccessToken()) {
    		try {
    			OAuthService.doAuthCodeRedirect(response, authRedirectUrl);
    			return;
    		} catch (OAuthSystemException e) {
    			throw new RuntimeException(e);
    		}
    	}
		
		// ** Everything is fine; we have a certificate and oauth session
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
		out.println("opener.loginComplete();");
		out.println("window.close();");
		out.println("</script>");
		out.println("</html>");
		out.close();
	}
}
