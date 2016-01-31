package au.org.massive.strudel_web;

import au.org.massive.strudel_web.job_control.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Provides settings for the application. Requires "strudel-web.properties" to be in the class path.
 * Settings objects are singletons, and configuration is loaded only once.
 *
 * @author jrigby
 */
public class Settings {

    private String OAUTH_REDIRECT;
    private ConfigurationRegistry CONFIGURATION_REGISTRY;

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
                    CONFIGURATION_REGISTRY.addSystemConfiguration(configId, c, i == 0);
                }
            }
        } catch (NullPointerException | InvalidJsonConfigurationException | IOException e) {
            throw new RuntimeException(e);
        }

        CONFIGURATION_REGISTRY.addSystemConfiguration("default", new StrudelSystemConfiguration("118.138.233.195"));
    }

    public ConfigurationRegistry getSystemConfigurations() {
        return CONFIGURATION_REGISTRY;
    }

    public String getOAuthRedirect() {
        return OAUTH_REDIRECT;
    }
}
