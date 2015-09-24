package au.org.massive.strudel_web;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by jason on 29/09/15.
 */
public class SSHCertSigningBackend {
    private final String NAME;
    private final URL OAUTH_AUTHORIZATION_ENDPOINT;
    private final URL OAUTH_TOKEN_ENDPOINT;
    private final URL SSH_API_ENDPOINT;
    private final String OAUTH_CLIENT_ID;
    private final String OAUTH_CLIENT_SECRET;

    public SSHCertSigningBackend(String name, String authEndpoint, String tokenEndpoint, String sshApiEndpoint, String clientId, String clientSecret) throws MalformedURLException {
        this(name, new URL(authEndpoint), new URL(tokenEndpoint), new URL(sshApiEndpoint), clientId, clientSecret);
    }

    public SSHCertSigningBackend(String name, URL authEndpoint, URL tokenEndpoint, URL sshApiEndpoint, String clientId, String clientSecret) {
        this.NAME = name;
        OAUTH_AUTHORIZATION_ENDPOINT = authEndpoint;
        OAUTH_TOKEN_ENDPOINT = tokenEndpoint;
        SSH_API_ENDPOINT = sshApiEndpoint;
        OAUTH_CLIENT_ID = clientId;
        OAUTH_CLIENT_SECRET = clientSecret;
    }

    public String getName() {
        return NAME;
    }

    public URL getOAuthAuthorisationEndpoint() {
        return OAUTH_AUTHORIZATION_ENDPOINT;
    }

    public URL getOAuthTokenEndpoint() {
        return OAUTH_TOKEN_ENDPOINT;
    }

    public URL getSshApiEndpoint() {
        return SSH_API_ENDPOINT;
    }

    public String getOAuthCliendId() {
        return OAUTH_CLIENT_ID;
    }

    public String getOAuthClientSecret() {
        return OAUTH_CLIENT_SECRET;
    }
}
