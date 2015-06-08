package au.org.massive.strudel_web.job_control;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a particular job to execute on the remote host.
 * @author jrigby
 *
 */
public class JobParameters {
	private final String remoteHost;
	private final String commandPattern;
	private final String resultRegexPattern;
	private final Map<String,String> defaultParams;
	private final Set<String> requiredParams;
	private final List<CommandPostprocessor> postprocessors;
	public JobParameters(String remoteHost, String commandPattern,
			String resultRegexPattern, Map<String,String> defaultParams,
			Set<String> requiredParams, List<CommandPostprocessor> postprocessors) {
		super();
		this.remoteHost = remoteHost;
		this.commandPattern = commandPattern;
		this.resultRegexPattern = resultRegexPattern;
		this.defaultParams = defaultParams;
		this.requiredParams = requiredParams;
		this.postprocessors = postprocessors;
	}
	public String getRemoteHost() {
		return remoteHost;
	}
	public String getCommandPattern() {
		return commandPattern;
	}
	public String getResultRegexPattern() {
		return resultRegexPattern;
	}
	public Map<String,String> getDefaultParams() {
		return defaultParams;
	}
	public Set<String> getRequiredParams() {
		return requiredParams;
	}
	public List<CommandPostprocessor> getPostprocessors() {
		return postprocessors;
	}
}
