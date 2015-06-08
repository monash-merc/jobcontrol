package au.org.massive.strudel_web.job_control;

/**
 * A {@link JobConfiguration} essentially maps a string reference to a {@link JobParameters} object.
 * The {@link JobParameters} object is used internally by {@link JobFactory} to produce a {@link JobFactory.Job}
 * object.
 * 
 * @author jrigby
 *
 */
public interface JobConfiguration {
	
	public JobParameters findByJobType(String jobType) throws NoSuchJobTypeException;
	
}
