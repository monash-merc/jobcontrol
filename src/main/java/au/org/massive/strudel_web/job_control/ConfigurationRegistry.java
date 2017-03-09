package au.org.massive.strudel_web.job_control;

import au.org.massive.strudel_web.SSHCertSigningBackend;
import au.org.massive.strudel_web.Settings;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Created by jason on 29/09/15.
 */
public class ConfigurationRegistry {
    private Map<String, SSHCertSigningBackend> authBackends;
    private Map<String, AbstractSystemConfiguration> systemConfigurations;

    public ConfigurationRegistry() {
        this.authBackends = new HashMap<>();
        this.systemConfigurations = new LinkedHashMap<>();
    }

    public void addSSHCertSigningBackend(String id, SSHCertSigningBackend backend) {
        addSSHCertSigningBackend(id, backend, false);
    }

    public void addSSHCertSigningBackend(String id, SSHCertSigningBackend backend, boolean setAsDefault) {
        authBackends.put(id, backend);
        if (setAsDefault || authBackends.size() == 1) {
            authBackends.put("default", backend);
        }
    }

    public SSHCertSigningBackend getSSHCertSigningBackendById(String id) {
        return authBackends.get(id);
    }

    /**
     * Gets the default SSH cert signing backend, which is either the one maked 'default', or the first one
     * in the map.
     * @return the default cert signing backend
     */
    public SSHCertSigningBackend getDefaultSSHCertSigningBackend() {
        return authBackends.getOrDefault("default", authBackends.get(authBackends.keySet().iterator().next()));
    }

    public void addSystemConfiguration(String id, AbstractSystemConfiguration configuration) {
        addSystemConfiguration(id, configuration, false);
    }

    public void addSystemConfiguration(String id, AbstractSystemConfiguration configuration, boolean setAsDefault) {
        systemConfigurations.put(id, configuration);
        if (setAsDefault || systemConfigurations.size() == 1) {
            systemConfigurations.put("default", configuration);
        }
    }

    public AbstractSystemConfiguration getSystemConfigurationById(String id) {
        return systemConfigurations.get(id);
    }

    /**
     * Gets the default SSH cert signing backend, which is either the one maked 'default', or the first one
     * in the map.
     * @return the default cert signing backend
     */
    public AbstractSystemConfiguration getDefaultSystemConfiguration() {
        return systemConfigurations.getOrDefault("default", systemConfigurations.get(systemConfigurations.keySet().iterator().next()));
    }

    public String getSystemConfigurationAsJson() {
        Gson gson = new Gson();
        Set<String> configurationKeys = systemConfigurations.keySet();
        configurationKeys.remove("default");
        HashMap<String, AbstractSystemConfiguration> systemConfigurationsCopy = new HashMap<>();
        for (String key : configurationKeys) {
            systemConfigurationsCopy.put(key, systemConfigurations.get(key));
        }
        return gson.toJson(systemConfigurationsCopy);
    }
}
