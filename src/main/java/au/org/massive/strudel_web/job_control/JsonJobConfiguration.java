package au.org.massive.strudel_web.job_control;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Created by jason on 21/09/15.
 */
public class JsonJobConfiguration extends AbstractJobConfiguration {

    public static JsonJobConfiguration getInstance(URL url) throws InvalidJsonConfigurationException, IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));

        StringBuilder jsonFile = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            jsonFile.append(inputLine);
        }
        in.close();

        return getInstance(jsonFile.toString());
    }

    public static JsonJobConfiguration getInstance(String jsonConfig) throws InvalidJsonConfigurationException {
        Gson gson = new Gson();
        @SuppressWarnings("unchecked") Map<String, Object> config = gson.fromJson(jsonConfig, HashMap.class);
        JsonJobConfiguration jsonJobConfiguration;
        if (config.containsKey("loginHost")) {
            jsonJobConfiguration = new JsonJobConfiguration((String) config.get("loginHost"));
        } else {
            throw new InvalidJsonConfigurationException("JSON configuration must define 'loginHost'");
        }
        jsonJobConfiguration.parseConfig(config);
        return jsonJobConfiguration;
    }

    private JsonJobConfiguration(String loginHost) {
        super(loginHost);
    }

    @SuppressWarnings("unchecked")
    private void parseConfig(Map<String, Object> config) throws InvalidJsonConfigurationException {
        if (!config.containsKey("tasks")) {
            throw new InvalidJsonConfigurationException("JSON configuration must define 'tasks'");
        }
        Map<String, Map<String,Object>> tasks = (Map<String, Map<String,Object>>) config.get("tasks");
        for (String taskName : tasks.keySet()) {

            Map<String,Object> task = tasks.get(taskName);

            String remoteHost = getLoginHost();
            if (task.containsKey("remoteHost")) {
                remoteHost = (String) task.get("remoteHost");
            }
            Map<String, String> defaults = new HashMap<String,String>();
            if (task.containsKey("defaults")) {
                defaults = (Map<String, String>) task.get("defaults");
            }
            Set<String> requiredParams = new HashSet<String>();
            if (task.containsKey("required")) {
                ArrayList<String> requiredParamsList = (ArrayList<String>) task.get("required");
                requiredParams = new HashSet<String>(requiredParams.size());
                requiredParams.addAll(requiredParamsList);
            }

            String commandPattern;
            if (task.containsKey("commandPattern")) {
                commandPattern = (String) task.get("commandPattern");
            } else {
                throw new InvalidJsonConfigurationException("JSON configuration for task '"+taskName+"' must define 'commandPattern'");
            }

            String resultsPattern;
            if (task.containsKey("resultPattern")) {
                resultsPattern = (String) task.get("resultPattern");
            } else {
                throw new InvalidJsonConfigurationException("JSON configuration for task '"+taskName+"' must define 'resultPattern'");
            }

            JobParameters taskParameters = new JobParameters(
                    remoteHost,
                    commandPattern,
                    resultsPattern,
                    defaults,
                    requiredParams,
                    new ArrayList<CommandPostprocessor>()
            );

           addConfiguration(taskName, taskParameters);
        }
    }
}
