package au.org.massive.strudel_web.job_control;

import au.org.massive.strudel_web.RegexHelper;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a JSON configuration file designed for the desktop app, and adapts it for web app use
 */
public class StrudelDesktopConfigurationAdapter extends HashMap<String, JsonSystemConfiguration> {

    /**
     * Constructor accepting JSON configuration as a string
     *
     * @param jsonConfig JSON configuration
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    public StrudelDesktopConfigurationAdapter(String jsonConfig) throws InvalidJsonConfigurationException {
        this("", jsonConfig);
    }

    /**
     * Constructor accepting a URL pointing to the JSON configuration
     *
     * @param url of the configuration file
     * @throws IOException thrown on network IO errors
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    public StrudelDesktopConfigurationAdapter(URL url) throws IOException, InvalidJsonConfigurationException {
        this("", url);
    }

    /**
     * Constructor accepting JSON configuration as a string
     *
     * @param configurationNamePrefix a string added to each configuration, e.g. the facility name
     * @param jsonConfig JSON configuration
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    public StrudelDesktopConfigurationAdapter(String configurationNamePrefix, String jsonConfig) throws InvalidJsonConfigurationException {
        parseConfig(configurationNamePrefix, jsonConfig);
    }

    /**
     * Constructor accepting a URL pointing to the JSON configuration
     *
     * @param configurationNamePrefix a string added to each configuration, e.g. the facility name
     * @param url of the configuration file
     * @throws IOException thrown on network IO errors
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    public StrudelDesktopConfigurationAdapter(String configurationNamePrefix, URL url) throws IOException, InvalidJsonConfigurationException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));

        StringBuilder jsonFile = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            jsonFile.append(inputLine);
        }
        in.close();

        parseConfig(configurationNamePrefix, jsonFile.toString());
    }

    /**
     * Iterates over each "flavour" in the JSON file and extracts the appropriate configuraiton parameters
     *
     * @param configurationNamePrefix a string to be prepended to each configuration. Used to avoid naming conflicts when multiple configurations are loaded.
     * @param jsonConfig JSON configuration text
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    private void parseConfig(String configurationNamePrefix, String jsonConfig) throws InvalidJsonConfigurationException {
        Gson gson = new Gson();
        System.out.println(jsonConfig);
        @SuppressWarnings("unchecked") Map<String, Map<String, Object>> configurations = (Map<String, Map<String, Object>>) ((List<Object>) gson.fromJson(jsonConfig, ArrayList.class)).get(1);
        for (String configurationName : configurations.keySet()) {
            put(configurationNamePrefix + configurationName, parseConfig(configurations.get(configurationName)));
        }
    }

    /**
     * Extracts the useful functions from the flavour
     *
     * @param config a {@link Map} of objects for each function
     * @return a system configuration object
     * @throws InvalidJsonConfigurationException thrown if invalid JSON configuration is encountered
     */
    private JsonSystemConfiguration parseConfig(Map<String, Object> config) throws InvalidJsonConfigurationException {

        String loginHost;
        if (config.containsKey("loginHost")) {
            loginHost = (String) config.get("loginHost");
        } else {
            throw new InvalidJsonConfigurationException("'loginHost' required in JSON configuration");
        }

        // Get defaults
        @SuppressWarnings("unchecked") Map<String, Object> jobDefaults = (Map<String, Object>) config.get("defaults");
        final int jobHours = ((Double) jobDefaults.get("jobParams_hours")).intValue();
        final int jobMem = ((Double) jobDefaults.get("jobParams_mem")).intValue();
        final int jobPPN = ((Double) jobDefaults.get("jobParams_ppn")).intValue();

        Map<String, Object> parsedConfig = new HashMap<>();
        parsedConfig.put("loginHost", loginHost);
        //noinspection unchecked
        parsedConfig.put("isTunnelTerminatedOnLoginHost", isTunnelTerminatedOnLoginHost(
                (String) ((Map<String, Object>) config.get("tunnel")).get("cmd")
        ));
        Map<String, Object> tasks = new HashMap<>();
        parsedConfig.put("tasks", tasks);

        Map<String, Object> function;
        String functionName;

        tasks.put("exechost", extractFunctionFromStrudelConfig("execHost", config));
        tasks.put("getprojects", extractFunctionFromStrudelConfig("getProjects", config));
        tasks.put("listall", extractFunctionFromStrudelConfig("listAll", config));
        tasks.put("otp", extractFunctionFromStrudelConfig("otp", config));
        tasks.put("running", extractFunctionFromStrudelConfig("running", config));

        Map<String, String> defaults = new HashMap<>();
        defaults.put("hours", String.valueOf(jobHours));
        defaults.put("ppn", String.valueOf(jobPPN));
        defaults.put("mem", String.valueOf(jobMem));
        tasks.put("startserver", extractFunctionFromStrudelConfig("startServer", config, defaults));

        tasks.put("stop", extractFunctionFromStrudelConfig("stop", config));
        tasks.put("stopforrestart", extractFunctionFromStrudelConfig("stopForRestart", config));
        tasks.put("vncdisplay", extractFunctionFromStrudelConfig("vncDisplay", config));

        return JsonSystemConfiguration.getInstance(parsedConfig);
    }

    /**
     * Attempts to determine whether the vnc tunnels need to terminate at the login or execution host
     * @param sshCommand the ssh command from the json file
     * @return true if the tunnel is terminated on the login host
     */
    private boolean isTunnelTerminatedOnLoginHost(String sshCommand) {
        return sshCommand.contains("{localPortNumber}:{execHost}:{remotePortNumber}");
    }

    /**
     * Converts the JSON object from the desktop app format to the web app format. By default, all parameters are set as requried.
     *
     * @param functionName the name of the function to convert
     * @param config       the configuration object
     * @return the converted configuration object
     */
    private static Map<String, Object> extractFunctionFromStrudelConfig(String functionName, Map<String, Object> config) {
        return extractFunctionFromStrudelConfig(functionName, config, new HashMap<String, String>(0));
    }

    /**
     * Converts the JSON object from the desktop app format to the web app format. By default, all parameters are set as requried.
     *
     * @param functionName the name of the function to convert
     * @param config       the configuration object
     * @param defaults     a Map of default parameter values
     * @return the converted configuration object
     */
    private static Map<String, Object> extractFunctionFromStrudelConfig(String functionName, Map<String, Object> config, Map<String, String> defaults) {
        Map<String, Object> function = new HashMap<>();
        String cmdPattern = getCommandPattern(functionName, config);
        function.put("commandPattern", cmdPattern);
        function.put("resultPattern", getResultPattern(functionName, config));
        function.put("required", getCommandPatternFields(cmdPattern));
        function.put("defaults", defaults);

        // The exec host is not known in advance, so if the command is to be targeted on the exec host, set the remote host as an empty string.
        // This will require the API endpoint to include a remote host - it becomes the client's responsibility to know this in advnace.
        //noinspection unchecked
        if (((Map<String, Object>) config.get(functionName)).get("host").equals("exec")) {
            function.put("remoteHost", "");
        }

        return function;
    }

    /**
     * Gets a command pattern, removing outer quotes and prefixing curly bracket value substitution with a dollar sign
     *
     * @param functionName function name corresponding to the command pattern
     * @param configObject configuration object
     * @return the converted command pattern
     */
    private static String getCommandPattern(String functionName, Map<String, Object> configObject) {
        // Remove enclosing quotes
        @SuppressWarnings("unchecked") String cmdPattern = (String) (((Map<String, Object>) configObject.get(functionName)).get("cmd"));
        return convertCommandPattern(cmdPattern.replaceAll("^\"(.*)\"$", "$1"));
    }

    /**
     * Gets all fields from a command pattern
     *
     * @param cmdPattern the command pattern
     * @return a list of field names
     */
    private static List<String> getCommandPatternFields(String cmdPattern) {
        Pattern pattern = Pattern.compile("\\$\\{([a-zA-Z]+)\\}");
        Matcher matcher = pattern.matcher(cmdPattern);
        List<String> fields = new LinkedList<>();
        while (matcher.find()) {
            fields.add(matcher.group(1));
        }
        return fields;
    }

    /**
     * Get the command result regex, converting named groups from the python notation to the java notation. If
     * there are no named groups, one is created that encloses the entire expression.
     *
     * @param functionName function name corresponding to the result regex
     * @param configObject configuration object
     * @return the converted regex
     */
    private static String getResultPattern(String functionName, Map<String, Object> configObject) {
        @SuppressWarnings("unchecked") String regex = ((List<String>) ((Map<String, Object>) configObject.get(functionName)).get("regex")).get(0);
        // by default, all output should be captured
        if (regex == null) {
            regex = ".+";
        }
        regex = convertRegex(regex);

        // If there are no named groups, enclose entire regex in a single "output" group
        if (RegexHelper.getNamedGroupCandidates(regex).size() == 0) {
            regex = "(?<output>" + regex + ")";
        }

        return regex;
    }

    /**
     * Fixes up python-style regex
     *
     * @param regex the regular expression
     * @return modified regular expression
     */
    private static String convertRegex(String regex) {
        return regex.replaceAll("(\\(\\?)P(<[a-zA-Z][a-zA-Z0-9]*>)", "$1$2");
    }

    /**
     * Converts the command pattern by prefixing curly-bracket string substitution with a dollar sign
     * and unescaping quotes
     *
     * @param commandPattern command pattern to convert
     * @return the converted command pattern
     */
    private static String convertCommandPattern(String commandPattern) {
        return commandPattern.replaceAll("(\\{[a-zA-Z]+\\})", "\\$$1").replace("\\\"", "\"");
    }
}
