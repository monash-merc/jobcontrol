package au.org.massive.strudel_web.job_control;

import java.util.List;
import java.util.Map;

/**
 * A {@link CommandPostprocessor} may be used to augment the behaviour of a command defined in an object that
 * subclasses {@link AbstractJobConfiguration}. The output of the initial command is filtered through a list
 * of {@link CommandPostprocessor} objects.
 * 
 * @author jrigby
 *
 */
public interface CommandPostprocessor {
	public List<Map<String,String>> process(List<Map<String,String>> input);
}
