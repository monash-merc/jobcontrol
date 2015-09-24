package au.org.massive.strudel_web.job_control;

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
 * @author jrigby
 *
 */
public abstract class AbstractSystemConfiguration implements TaskConfiguration {
	
	private final Map<String,TaskParameters> configurations;
	private final String loginHost;
	
	public AbstractSystemConfiguration(String loginHost) {
		configurations = new HashMap<String,TaskParameters>();
		this.loginHost = loginHost;
	}
	
	protected String getLoginHost() {
		return loginHost;
	}
	
	protected void addConfiguration(String jobName, Map<String,String> defaults,
			String[] requiredParams, String commandPattern, String resultPattern) {
		addConfiguration(getLoginHost(), jobName, defaults, requiredParams, commandPattern, resultPattern, new LinkedList<CommandPostprocessor>());
	}
	
	protected void addConfiguration(String jobName, Map<String,String> defaults,
			Set<String> requiredParams, String commandPattern, String resultPattern) {
		addConfiguration(getLoginHost(), jobName, defaults, requiredParams, commandPattern, resultPattern, new LinkedList<CommandPostprocessor>());
	}
	
	protected void addConfiguration(String host, String jobName, Map<String,String> defaults,
			String[] requiredParams, String commandPattern, String resultPattern) {
		addConfiguration(host, jobName, defaults, requiredParams, commandPattern, resultPattern, new LinkedList<CommandPostprocessor>());
	}
	
	protected void addConfiguration(String host, String jobName, Map<String,String> defaults,
			Set<String> requiredParams, String commandPattern, String resultPattern) {
		configurations.put(jobName,
				new TaskParameters(host, commandPattern, resultPattern, defaults, requiredParams, new LinkedList<CommandPostprocessor>()));
	}
	
	protected void addConfiguration(String jobName, Map<String,String> defaults,
			String[] requiredParams, String commandPattern, String resultPattern, List<CommandPostprocessor> postprocessors) {
		addConfiguration(getLoginHost(), jobName, defaults, requiredParams, commandPattern, resultPattern, postprocessors);
	}
	
	protected void addConfiguration(String jobName, Map<String,String> defaults,
			Set<String> requiredParams, String commandPattern, String resultPattern, List<CommandPostprocessor> postprocessors) {
		addConfiguration(getLoginHost(), jobName, defaults, requiredParams, commandPattern, resultPattern, postprocessors);
	}
	
	protected void addConfiguration(String host, String jobName, Map<String,String> defaults,
			String[] requiredParams, String commandPattern, String resultPattern, List<CommandPostprocessor> postprocessors) {
		
		Set<String> requiredParamsSet;
		if (requiredParams == null || requiredParams.length == 0) {
			requiredParamsSet = new HashSet<String>();
		} else {
			requiredParamsSet = new HashSet<String>(Arrays.asList(requiredParams));
		}
		
		addConfiguration(host, jobName, defaults, requiredParamsSet, commandPattern, resultPattern, postprocessors);
	}
	
	protected void addConfiguration(String host, String jobName, Map<String,String> defaults,
			Set<String> requiredParams, String commandPattern, String resultPattern, List<CommandPostprocessor> postprocessors) {
		
		if (defaults == null) {
			defaults = new HashMap<String,String>();
		}
		if (requiredParams == null) {
			requiredParams = new HashSet<String>();
		}

		addConfiguration(jobName, new TaskParameters(host, commandPattern, resultPattern, defaults, requiredParams, postprocessors));
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
