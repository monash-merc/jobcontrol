package au.org.massive.strudel_web.job_control;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.StrSubstitutor;

import au.org.massive.strudel_web.RegexHelper;
import au.org.massive.strudel_web.Session;
import au.org.massive.strudel_web.ssh.ForkedSSHClient;
import au.org.massive.strudel_web.ssh.SSHClient;
import au.org.massive.strudel_web.ssh.SSHExecException;

import com.google.gson.Gson;

/**
 * Produces {@link Task} objects based on a given {@link TaskConfiguration} and job type.
 * @author jrigby
 *
 */
public class TaskFactory {
	
	private final TaskConfiguration config;
	
	public TaskFactory(TaskConfiguration config) {
		this.config = config;
	}
	
	public Task getInstance(String taskType, Session session) throws IOException, NoSuchTaskTypeException {
		TaskParameters params = config.findByTaskType(taskType);
		return new Task(new ForkedSSHClient(session.getCertificate(),params.getRemoteHost()),
				params.getCommandPattern(), params.getResultRegexPattern(), params.getDefaultParams(),
				params.getRequiredParams(), params.getPostprocessors());
	}

	public static class Task {
		
		private final SSHClient sshClient;
		private final String commandPattern;
		private final String resultRegexPattern;
		private final Map<String,String> defaultParams;
		private final Set<String> requiredParams;
		private final List<CommandPostprocessor> postprocessors;
		
		private Task(SSHClient sshClient, String commandPattern, String resultRegexPattern, Map<String, String> defaultParams, Set<String> requiredParams, List<CommandPostprocessor> postprocessors) {
			this.sshClient = sshClient;
			this.commandPattern = commandPattern;
			this.resultRegexPattern = resultRegexPattern;
			this.defaultParams = defaultParams;
			this.requiredParams = requiredParams;
			this.postprocessors = postprocessors;
		}
		
		public List<Map<String, String>> run(Map<String, String> parameters) throws IOException, SSHExecException, MissingRequiredTaskParametersException {
			List<Map<String,String>> result = RegexHelper.processRegexForEachLine(resultRegexPattern, sshClient.exec(createCommand(commandPattern, parameters, defaultParams, requiredParams)));
			for (CommandPostprocessor p : postprocessors) {
				result = p.process(result);
			}
			return result;
		}
		
		public String runJsonResult(Map<String, String> parameters) throws IOException, SSHExecException, MissingRequiredTaskParametersException {
			return new Gson().toJson(run(parameters));
		}
		
		private static String createCommand(String commandPattern, Map<String,String> params, Map<String,String> defaultParams, Set<String> requiredParams) throws MissingRequiredTaskParametersException {
			if (requiredParams != null) {
				// Verify all required parameters are present
				Set<String> suppliedParams = new HashSet<String>();
				suppliedParams.addAll(params.keySet());
				if (defaultParams != null) {
					suppliedParams.addAll(defaultParams.keySet());
				}
				if (!suppliedParams.containsAll(requiredParams)) {
					StringBuilder sb = new StringBuilder();
					sb.append("The following parameters are required: ");
					Iterator<String> it = requiredParams.iterator();
					while (it.hasNext()) {
						sb.append(it.next());
						if (it.hasNext()) {
							sb.append(", ");
						}
					}
					throw new MissingRequiredTaskParametersException(sb.toString());
				}
			}
			
			// Replace user supplied parameters
			StrSubstitutor sub = new StrSubstitutor(makeParamsSafe(params));
			String result = sub.replace(commandPattern);
			
			if (defaultParams != null) {
				// Fill in the rest with defaults
				sub = new StrSubstitutor(makeParamsSafe(defaultParams));
				return sub.replace(result);
			} else {
				return result;
			}
		}
		
		private static Map<String,String> makeParamsSafe(Map<String,String> params) {
			Map<String,String> safeValues = new HashMap<String,String>();
			for (String key : params.keySet()) {
				String value = "'"+params.get(key).replace("'", "\\'")+"'";
				safeValues.put(key, value);
			}
			return safeValues;
		}
	}
}