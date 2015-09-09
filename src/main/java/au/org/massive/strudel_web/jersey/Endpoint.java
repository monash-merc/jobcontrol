package au.org.massive.strudel_web.jersey;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.org.massive.strudel_web.Session;

/**
 * Provides some convenience methods for api endpoints to get sessions that are appropriately authorised.
 * @author jrigby
 *
 */
public abstract class Endpoint {
	
	protected Session getSession(HttpServletRequest request) {
		return new Session(request);
	}
	
	protected Session getSessionWithCertificateOrSendError(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Session session = getSession(request);
		if (session != null) {
			if (!session.hasCertificate()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Session requires a valid SSH certificate. Access the /api/register_key endpoint first.");
	    		return null;
			} else {
				return session;
			}
		}
		return null;
	}
}
