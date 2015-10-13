package au.org.massive.strudel_web.job_control;

import au.org.massive.strudel_web.Settings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a set of convenience methods for initialising a {@link TaskConfiguration} object.
 * All {@link TaskConfiguration} objects should ideally subclass {@link AbstractSystemConfiguration}.
 *
 * @author jrigby
 */
public abstract class AbstractSystemConfiguration implements TaskConfiguration {

    // This list is there only for the /api/configurations endpoint
    // and is only used by the client.
    private final List<String> authBackendNames;

    private final Map<String, TaskParameters> configurations;
    private final String loginHost;

    public AbstractSystemConfiguration(String loginHost) {
        authBackendNames = new LinkedList<>();
        configurations = new HashMap<>();
        this.loginHost = loginHost;
    }

    protected String getLoginHost() {
        return loginHost;
    }

    public void addAuthBackend(String name) {
        authBackendNames.add(name);
    }

    protected void addConfiguration(String jobName, Map<String, String> defaults,
                                    String[] requiredParams, String commandPattern, String resultPattern) {
        addConfiguration(getLoginHost(), jobName, defaults, requiredParams, commandPattern, resultPattern);
    }

    protected void addConfiguration(String host, String jobName, Map<String, String> defaults,
                                    String[] requiredParams, String commandPattern, String resultPattern) {

        Set<String> requiredParamsSet;
        if (requiredParams == null || requiredParams.length == 0) {
            requiredParamsSet = new HashSet<>();
        } else {
            requiredParamsSet = new HashSet<>(Arrays.asList(requiredParams));
        }

        addConfiguration(host, jobName, defaults, requiredParamsSet, commandPattern, resultPattern);
    }

    protected void addConfiguration(String host, String jobName, Map<String, String> defaults,
                                    Set<String> requiredParams, String commandPattern, String resultPattern) {

        if (defaults == null) {
            defaults = new HashMap<>();
        }
        if (requiredParams == null) {
            requiredParams = new HashSet<>();
        }

        addConfiguration(jobName, new TaskParameters(host, commandPattern, resultPattern, defaults, requiredParams));
    }

    protected void addConfiguration(String jobName, TaskParameters job) {
        configurations.put(jobName, job);
    }

    @Override
    public TaskParameters findByTaskType(String jobType)
            throws NoSuchTaskTypeException {
        String searchString = jobType.toLowerCase();

        if (!configurations.containsKey(searchString)) {
            throw new NoSuchTaskTypeException();
        }

        return configurations.get(searchString);
    }

}
