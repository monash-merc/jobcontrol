package au.org.massive.strudel_web.jersey;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import au.org.massive.strudel_web.NoSuchSessionException;
import au.org.massive.strudel_web.OAuthService;
import au.org.massive.strudel_web.Session;

/**
 * OAuth callback endpoints
 * @author jrigby
 *
 */
@Path("oauth")
public class OAuthEndpoints extends Endpoint {

	@GET
    @Path("callback")
    public Response authorize(@Context HttpServletRequest req, @Context HttpServletResponse res) throws IOException {
    	// Extract token from state parameter (i.e. redirect URL)
		try {
    		Session session;
			try {
				session = new Session(getSessionIdFromRedirectUrl(req.getParameter("state")));
			} catch (NoSuchSessionException e) {
				res.sendError(HttpServletResponse.SC_BAD_REQUEST, "OAuth2 redirect error");
				return null;
			}
    		OAuthService.doTokenRequest(req, session);
			return Response.seeOther(OAuthService.getAfterAuthRedirect(req)).build();
		} catch (OAuthProblemException e) {
			e.printStackTrace();
			URI redirect = UriBuilder.fromUri(req.getContextPath()).path("login-failed.html").build();

			return Response.seeOther(redirect).build();
		} catch (OAuthSystemException e) {
			e.printStackTrace();
		}
    	return Response.serverError().build();
    }
	
	private String getSessionIdFromRedirectUrl(String redirectUrl) {
		final String searchString = "?token=";
		int indexOfToken = redirectUrl.toLowerCase().indexOf(searchString);
		if (indexOfToken < 0) {
			return "";
		}
		
		String token = redirectUrl.substring(indexOfToken+searchString.length());
		if (token.contains("&")) {
			token = token.split("&")[0];
		}
		
		return token;
	}
}
