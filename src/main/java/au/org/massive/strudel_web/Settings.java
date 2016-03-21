package au.org.massive.strudel_web;

import au.org.massive.strudel_web.job_control.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.net.ssl.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Provides settings for the application. Requires "strudel-web.properties" to be in the class path.
 * Settings objects are singletons, and configuration is loaded only once.
 *
 * @author jrigby
 */
public class Settings {

    private String OAUTH_REDIRECT;
    private ConfigurationRegistry CONFIGURATION_REGISTRY;

    private String BASE_URL;
    private InternetAddress FEEDBACK_TO_ADDRESS;
    private InternetAddress FEEDBACK_FROM_ADDRESS;
    private String SMTP_HOST;
    private int SMTP_PORT;
    private String FEEDBACK_EMAIL_SUBJECT;

    private static Settings instance;

    private Settings() {
        Configuration config;
        try {
            config = new PropertiesConfiguration("strudel-web.properties");
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        // Turn of SSL cert verification if requested
        if (config.getBoolean("allow-invalid-ssl-cert", false)) {
            try {
                SSLContext sc = SSLContext.getInstance("SSL");

                sc.init(null,
                        new TrustManager[] {
                                new X509TrustManager() {
                                    @Override
                                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                                    }

                                    @Override
                                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                                    }

                                    @Override
                                    public X509Certificate[] getAcceptedIssuers() {
                                        return null;
                                    }
                                }
                        },
                        new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

                HostnameVerifier allHostsValid = new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };

                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            } catch (KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        SMTP_HOST = config.getString("smtp-host", null);
        SMTP_PORT = config.getInt("smtp-port", 25);
        try {
            FEEDBACK_TO_ADDRESS = config.getString("feedback-to-address", null) != null ? new InternetAddress(config.getString("feedback-to-address")) : null;
            FEEDBACK_FROM_ADDRESS = new InternetAddress(config.getString("feedback-from-address", "noreply@example.com"));
        } catch (AddressException e) {
            throw new RuntimeException(e);
        }
        FEEDBACK_EMAIL_SUBJECT = config.getString("feedback-email-subject", "Feedback for Strudel Web");
        BASE_URL = config.getString("base-url", null);
        setupSystemConfigurations(config);
    }

    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    private void setupSystemConfigurations(Configuration config) {
        OAUTH_REDIRECT = config.getString("oauth-redirect");

        CONFIGURATION_REGISTRY = new ConfigurationRegistry();

        // Define all SSH certificate signing servers
        try {
            String sshCertBackendName;
            for (int i = 0; (sshCertBackendName = config.getString("ssh-cert-backend-name" + i, (i == 0) ? "default" : null)) != null; i++) {
                CONFIGURATION_REGISTRY.addSSHCertSigningBackend(sshCertBackendName,
                        new SSHCertSigningBackend(
                                sshCertBackendName,
                                config.getString("ssh-cert-backend-oauth-authorization-endpoint" + i),
                                config.getString("ssh-cert-backend-oauth-token-endpoint" + i),
                                config.getString("ssh-cert-backend-ssh-api-endpoint" + i),
                                config.getString("ssh-cert-backend-oauth-client-id" + i),
                                config.getString("ssh-cert-backend-oauth-client-secret" + i)
                        )
                );
            }
        } catch (MalformedURLException | NullPointerException e) {
            e.printStackTrace();
        }

        // Define all remote system configurations
        try {
            String systemConfigName;
            for (int i = 0; (systemConfigName = config.getString("system-configuration-name"+i)) != null; i++) {
                URL jsonConfigUrl = new URL(config.getString("system-configuration-json-url"+i));
                StrudelDesktopConfigurationAdapter strudelConfig = new StrudelDesktopConfigurationAdapter(systemConfigName+"|", jsonConfigUrl);
                for (String configId : strudelConfig.keySet()) {
                    AbstractSystemConfiguration c = strudelConfig.get(configId);
                    for (Object o : config.getList("system-configuration-auth-backends"+i)) {
                        c.addAuthBackend((String) o);
                    }
                    CONFIGURATION_REGISTRY.addSystemConfiguration(configId, c);
                }
            }
        } catch (NullPointerException | InvalidJsonConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigurationRegistry getSystemConfigurations() {
        return CONFIGURATION_REGISTRY;
    }

    public String getOAuthRedirect() {
        return OAUTH_REDIRECT;
    }

    public Properties getSMTPProperties() {
        Properties properties = new Properties();
        if (FEEDBACK_TO_ADDRESS != null && SMTP_HOST != null) {
            properties.put("mail.smtp.host", SMTP_HOST);
            properties.put("mail.smtp.port", SMTP_PORT);
        }
        return properties;
    }

    public InternetAddress getFeedbackFromAddress() {
        return FEEDBACK_FROM_ADDRESS;
    }

    public InternetAddress getFeedbackToAddress() {
        return FEEDBACK_TO_ADDRESS;
    }

    public String getFeedbackEmailSubject() {
        return FEEDBACK_EMAIL_SUBJECT;
    }

    public boolean isFeedbackEmailEnabled() {
        return FEEDBACK_TO_ADDRESS != null;
    }

    public String getBaseUrl() {
        return BASE_URL;
    }
}
