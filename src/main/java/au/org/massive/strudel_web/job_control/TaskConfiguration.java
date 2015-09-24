package au.org.massive.strudel_web.job_control;

/**
 * A {@link TaskConfiguration} essentially maps a string reference to a {@link TaskParameters} object.
 * The {@link TaskParameters} object is used internally by {@link TaskFactory} to produce a {@link TaskFactory.Task}
 * object.
 * 
 * @author jrigby
 *
 */
public interface TaskConfiguration {
	
	public TaskParameters findByTaskType(String jobType) throws NoSuchTaskTypeException;
	
}
