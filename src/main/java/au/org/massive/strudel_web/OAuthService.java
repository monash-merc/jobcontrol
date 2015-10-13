package au.org.massive.strudel_web;

import java.io.IOException;
import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.jwt.JWT;
import org.apache.oltu.oauth2.jwt.io.JWTReader;

/**
 * Provides methods for the OAuth2 auth flow
 *
 * @author jrigby
 */
public class OAuthService {

    private static final Settings settings = Settings.getInstance();

    private OAuthService() {

    }

    /**
     * Generates an authorization code request
     *
     * @param afterAuthRedirect
     * @return an authorization code request
     * @throws OAuthSystemException
     */
    public static OAuthClientRequest getAuthCodeRequest(SSHCertSigningBackend authBackend, URI afterAuthRedirect) throws OAuthSystemException {
        return OAuthClientRequest.authorizationLocation(authBackend.getOAuthAuthorisationEndpoint().toString())
                .setClientId(authBackend.getOAuthCliendId())
                .setResponseType("code")
                .setRedirectURI(settings.getOAuthRedirect())
                .setState(afterAuthRedirect.toString())
                .buildQueryMessage();
    }

    /**
     * Performs the auth code redirect
     *
     * @param response
     * @param afterAuthRedirect
     * @throws OAuthSystemException
     */
    public static void doAuthCodeRedirect(HttpServletResponse response, Session session, URI afterAuthRedirect) throws OAuthSystemException {
        OAuthClientRequest oauthReq = OAuthService.getAuthCodeRequest(session.getSSHCertSigningBackend(), afterAuthRedirect);
        try {
            response.sendRedirect(URI.create(oauthReq.getLocationUri()).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the auth code from the request
     *
     * @param req
     * @return the auth code
     * @throws OAuthProblemException
     */
    public static String getAuthCodeFromRequest(HttpServletRequest req) throws OAuthProblemException {
        return OAuthAuthzResponse.oauthCodeAuthzResponse(req).getCode();
    }

    /**
     * Extracts the location to redirect after successful auth
     *
     * @param req
     * @return
     * @throws OAuthProblemException
     */
    public static URI getAfterAuthRedirect(HttpServletRequest req) throws OAuthProblemException {
        return URI.create(OAuthAuthzResponse.oauthCodeAuthzResponse(req).getState());
    }

    /**
     * Requests an access token
     *
     * @param req
     * @param session
     * @throws OAuthProblemException
     * @throws OAuthSystemException
     */
    public static void doTokenRequest(HttpServletRequest req, Session session) throws OAuthProblemException, OAuthSystemException {
        doTokenRequest(getAuthCodeFromRequest(req), session);
    }

    /**
     * Requests an access token
     *
     * @param code
     * @param session
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     */
    public static void doTokenRequest(String code, Session session) throws OAuthSystemException, OAuthProblemException {
        SSHCertSigningBackend authBackend = session.getSSHCertSigningBackend();
        String authHeader = "Basic " + new String(Base64.encodeBase64((authBackend.getOAuthCliendId() + ":" + authBackend.getOAuthClientSecret()).getBytes()));
        OAuthClient client = new OAuthClient(new URLConnectionClient());
        OAuthClientRequest req = OAuthClientRequest.tokenLocation(authBackend.getOAuthTokenEndpoint().toString())
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setRedirectURI(settings.getOAuthRedirect())
                .setCode(code).buildBodyMessage();
        req.setHeader("Accept", "application/json");
        req.setHeader("Authorization", authHeader);
        String accessToken = client.accessToken(req).getAccessToken();
        session.setOAuthAccessToken(accessToken);

        JWTReader jwtReader = new JWTReader();
        JWT jwtToken = jwtReader.read(accessToken);
        session.setUserEmail(jwtToken.getClaimsSet().getCustomField("email", String.class));
    }

}