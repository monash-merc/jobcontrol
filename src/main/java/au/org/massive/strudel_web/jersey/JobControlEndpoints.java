
package au.org.massive.strudel_web.jersey;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import au.org.massive.strudel_web.vnc.GuacamoleDB;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;

import au.org.massive.strudel_web.KeyService;
import au.org.massive.strudel_web.NoSuchSessionException;
import au.org.massive.strudel_web.Session;
import au.org.massive.strudel_web.SessionManager;
import au.org.massive.strudel_web.Settings;
import au.org.massive.strudel_web.UnauthorizedException;
import au.org.massive.strudel_web.job_control.JobFactory;
import au.org.massive.strudel_web.job_control.MissingRequiredJobParametersException;
import au.org.massive.strudel_web.job_control.NoSuchJobTypeException;
import au.org.massive.strudel_web.job_control.JobFactory.Job;
import au.org.massive.strudel_web.ssh.SSHExecException;
import au.org.massive.strudel_web.vnc.GuacamoleSession;
import au.org.massive.strudel_web.vnc.GuacamoleSessionManager;

import com.google.gson.Gson;

/**
 * Endpoints that act on an HPC system
 * 
 * @author jrigby
 *
 */
@Path("/")
public class JobControlEndpoints extends Endpoint{
	
	private static Settings settings = Settings.getInstance();
	
	/**
	 * Gets (and creates, if necessary) a session and returns the id and whether the session
	 * currently has a certificate associated with it.
	 * @param request
	 * @return a json object with some session info
	 */
    @GET
    @Path("session_info")
    @Produces("application/json")
    public String getSessionInfo(@Context HttpServletRequest request) {
    	Gson gson = new Gson();
    	Map<String,String> response = new HashMap<String,String>();
    	String sessionId = request.getSession(true).getId();
    	Session session;
		try {
			session = new Session(sessionId);
		} catch (NoSuchSessionException e1) {
			// If the server restarts and a client has a stale session, a new one is created
			request.getSession().invalidate();
			return getSessionInfo(request);
		}
    	response.put("session_id", sessionId);
		response.put("has_certificate", String.valueOf(session.hasCertificate()));
    	if (session.hasCertificate()) {
    		response.put("uid", session.getCertificate().getUserName());
    	} else {
    		response.put("uid", "");
    	}
    	response.put("has_oauth_access_token", String.valueOf(session.hasOAuthAccessToken()));
    	if (session.hasUserEmail()) {
    		response.put("email", session.getUserEmail());
    	} else {
    		response.put("email", "");
    	}
    	return gson.toJson(response);
    }
    
    /**
     * Gets a key pair and get the public key signed
     * @param request
     * @param response
     * @return a status message
     * @throws IOException
     */
    @GET
    @Path("register_key")
    @Produces("application/json")
    public String registerKey(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	Session session = getSession(request);
    	
    	if (!session.hasOAuthAccessToken()) {
    		response.sendError(HttpServletResponse.SC_FORBIDDEN, "No access token for the SSH AuthZ server in session");
			return null;
    	} else {
    		try {
				session.setCertificate(KeyService.registerKey(session.getOAuthAccessToken()));
			} catch (OAuthSystemException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return null;
			} catch (OAuthProblemException | UnauthorizedException e) {
				session.clearOAuthAccessToken();
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "The user did not authorise this request during the OAuth2 flow, or authorization has expired.");
				return null;
			}
    	}
    	
    	Gson gson = new Gson();
    	Map<String,String> responseMessage = new HashMap<String,String>();
    	responseMessage.put("status", "OK");
    	responseMessage.put("message", "Key pair generated and public key signed successfully");
    	return gson.toJson(responseMessage);
    }
    
    /**
     * Triggers a session logout
     * @param request
     * @param response
     * @return a status message
     * @throws IOException
     */
    @GET
    @Path("end_session")
    @Produces("application/json")
    public String invalidateSession(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	Gson gson = new Gson();
    	
    	Session session = getSession(request);
		SessionManager.endSession(session);
		
    	Map<String,String> responseMessage = new HashMap<String,String>();
    	responseMessage.put("message", "Session "+session.getSessionId()+" invalidated");
    	
    	return gson.toJson(responseMessage);
    }
    
    /**
     * Runs preconfigured commands on the remote HPC system. These commands are defined as part of a {@link au.org.massive.strudel_web.job_control.JobConfiguration} object.
     * @param job
     * @param request
     * @param response
     * @return the result of the command
     * @throws IOException
     * @throws SSHExecException
     */
    @GET
    @Path("/execute/{job}/")
    @Produces("application/json")
    public String executeJob(@PathParam("job") String job, @Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException, SSHExecException {
    	Session session = getSessionWithCertificateOrSendError(request, response);
    	if (session == null) {
    		return null;
    	}
    	
    	Job remoteJob;
		try {
			remoteJob = new JobFactory(settings.getJobConfiguration()).getInstance(job, session);
		} catch (NoSuchJobTypeException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}
    	
    	Map<String,String> parameters = new HashMap<String,String>();
    	for (String key : request.getParameterMap().keySet()) {
    		String value = request.getParameterMap().get(key)[0]; // Only one value is accepted
    		parameters.put(key, value);
    	}
    	
    	try {
			return remoteJob.runJsonResult(parameters);
		} catch (MissingRequiredJobParametersException e) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return null;
		}
    }
    
    /**
     * Starts a VNC tunnel for use with a Guacamole server
     * @param desktopName
     * @param vncPassword
     * @param remoteHost
     * @param display
     * @param request
     * @param response
     * @return a vnc session id and desktop name
     * @throws IOException
     */
    @GET
    @Path("/startvnctunnel")
    @Produces("application/json")
    public String startVncTunnel(
    		@QueryParam("desktopname") String desktopName,
    		@QueryParam("vncpassword") String vncPassword,
    		@QueryParam("remotehost") String remoteHost,
    		@QueryParam("display") int display,
    		@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	Session session = getSessionWithCertificateOrSendError(request, response);
    	if (session == null) {
    		return null;
    	}
    	int remotePort = display + 5900;
    	GuacamoleSession guacSession = GuacamoleSessionManager.startSession(desktopName, vncPassword, remoteHost, remotePort, session);
    	
    	Gson gson = new Gson();
    	Map<String,Object> responseData = new HashMap<String,Object>();
    	responseData.put("id", guacSession.getId());
    	responseData.put("desktopName", desktopName);
    	return gson.toJson(responseData);
    }

	@GET
	@Path("/updatevncpwd")
	@Produces("application/json")
	public String updateVncPassword(@QueryParam("desktopname") String desktopName,
									@QueryParam("vncpassword") String newPassword,
									@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
		Session session = getSessionWithCertificateOrSendError(request, response);
		if (session == null) {
			return null;
		}

		boolean done = false;
		for (GuacamoleSession s : session.getGuacamoleSessionsSet()) {
			if (s.getName().equals(desktopName)) {
				s.setPassword(newPassword);
				try {
					GuacamoleDB.updateSession(s);
				} catch (SQLException e) {
					e.printStackTrace();
					break;
				}
				done = true;
				break;
			}
		}

		Gson gson = new Gson();
		Map<String,Object> responseData = new HashMap<String,Object>();
		if (done) {
			responseData.put("message", "password updated");
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "could not update vnc password");
			return null;
		}
		return gson.toJson(responseData);
	}
    
    /**
     * Stops a guacamole VNC session
     * @param guacSessionId
     * @param request
     * @param response
     * @return a status message
     * @throws IOException
     */
    @GET
    @Path("/stopvnctunnel")
    @Produces("application/json")
    public String stopVncTunnel(
    		@QueryParam("id") int guacSessionId,
    		@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	Session session = getSessionWithCertificateOrSendError(request, response);
    	if (session == null) {
    		return null;
    	}
    	
    	GuacamoleSession guacSession = null;
    	for (GuacamoleSession s : session.getGuacamoleSessionsSet()) {
    		if (s.getId() == guacSessionId) {
    			guacSession = s;
    			break;
    		}
    	}
    	
    	Gson gson = new Gson();
    	Map<String,String> responseData = new HashMap<String,String>();
    	if (guacSession == null) {
    		response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No active session found by supplied ID");
    		return null;
    	} else {
    		GuacamoleSessionManager.endSession(guacSession, session);
    		responseData.put("message", "session deleted");
    	}
    	return gson.toJson(responseData);
    }
    
    /**
     * Lists all active VNC sessions for the current user
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    @GET
    @Path("/listvnctunnels")
    @Produces("application/json")
    public String listVncTunnels(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException {
    	Session session = getSessionWithCertificateOrSendError(request, response);
    	if (session == null) {
    		return null;
    	}
    	
    	List<Map<String,Object>> tunnels = new ArrayList<Map<String,Object>>(session.getGuacamoleSessionsSet().size());
    	for (GuacamoleSession s : session.getGuacamoleSessionsSet()) {
    		Map<String,Object> tunnel = new HashMap<String,Object>();
    		tunnels.add(tunnel);
    		
    		tunnel.put("id", s.getId());
    		tunnel.put("desktopName", s.getName());
    		tunnel.put("password", s.getPassword());
    	}
    	
    	Gson gson = new Gson();
    	return gson.toJson(tunnels);
    }
}