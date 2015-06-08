package au.org.massive.strudel_web;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import au.org.massive.strudel_web.job_control.JobConfiguration;
import au.org.massive.strudel_web.job_control.StrudelJobConfiguration;

/**
 * Provides settings for the application. Requires "strudel-web.properties" to be in the class path.
 * Settings objects are singletons, and configuration is loaded only once.
 * 
 * @author jrigby
 *
 */
public class Settings {
	
	private final String OAUTH_AUTHORIZATION_ENDPOINT;
	private final String OAUTH_TOKEN_ENDPOINT;
	private final String OAUTH_CLIENT_ID;
	private final String OAUTH_CLIENT_SECRET;
	private final String OAUTH_REDIRECT;
	private final String SSH_API_ENDPOINT;
	private final JobConfiguration JOB_CONFIGURATION;
	private final String GUACD_HOST;
	private final String GUAC_MYSQL_HOST;
	private final int GUAC_MYSQL_PORT;
	private final String GUAC_MYSQL_USER_NAME;
	private final String GUAC_MYSQL_PASSWORD;
	private final String GUAC_MYSQL_DB_NAME;
	
	private static Settings instance;
	
	private Settings() {
		try {
			Configuration config = new PropertiesConfiguration("strudel-web.properties");
			
			OAUTH_AUTHORIZATION_ENDPOINT = config.getString("oauth-authorization-endpoint");
			OAUTH_TOKEN_ENDPOINT = config.getString("oauth-token-endpoint");
			OAUTH_CLIENT_ID = config.getString("oauth-client-id");
			OAUTH_CLIENT_SECRET = config.getString("oauth-client-secret");
			OAUTH_REDIRECT = config.getString("oauth-redirect");
			
			SSH_API_ENDPOINT = config.getString("ssh-api-endpoint");
			JOB_CONFIGURATION = new StrudelJobConfiguration(config.getString("login-host"));
			
			GUACD_HOST = config.getString("guacd-host", "localhost");
			GUAC_MYSQL_HOST = config.getString("guac-mysql-host", "localhost");
			GUAC_MYSQL_PORT = config.getInt("guac-mysql-port", 3306);
			GUAC_MYSQL_USER_NAME = config.getString("guac-mysql-user-name", "guacamole");
			GUAC_MYSQL_PASSWORD = config.getString("guac-mysql-password");
			GUAC_MYSQL_DB_NAME = config.getString("guac-mysql-db-name", "guacamole");
			
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Settings getInstance() {
		if (instance == null) {
			instance = new Settings();
		}
		return instance;
	}

	public String getOAuthAuthorizationEndpoint() {
		return OAUTH_AUTHORIZATION_ENDPOINT;
	}

	public String getOAuthTokenEndpoint() {
		return OAUTH_TOKEN_ENDPOINT;
	}

	public String getOAuthClientId() {
		return OAUTH_CLIENT_ID;
	}

	public String getOAuthClientSecret() {
		return OAUTH_CLIENT_SECRET;
	}

	public String getOAuthRedirect() {
		return OAUTH_REDIRECT;
	}

	public String getSSHAPIEndpoint() {
		return SSH_API_ENDPOINT;
	}

	public JobConfiguration getJobConfiguration() {
		return JOB_CONFIGURATION;
	}
	
	public String getGuacdHost() {
		return GUACD_HOST;
	}

	public String getGuacMySQLHost() {
		return GUAC_MYSQL_HOST;
	}

	public int getGuacMySQLPort() {
		return GUAC_MYSQL_PORT;
	}

	public String getGuacMySQLUserName() {
		return GUAC_MYSQL_USER_NAME;
	}

	public String getGuacMySQLPassword() {
		return GUAC_MYSQL_PASSWORD;
	}

	public String getGuacMySQLDBName() {
		return GUAC_MYSQL_DB_NAME;
	}
}
