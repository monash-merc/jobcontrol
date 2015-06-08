package au.org.massive.strudel_web.job_control;

/**
 * Thrown if the job requested doesn't exist
 * @author jrigby
 *
 */
public class NoSuchJobTypeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7254797047028370205L;

	public NoSuchJobTypeException() {
		super();
	}

	public NoSuchJobTypeException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NoSuchJobTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoSuchJobTypeException(String message) {
		super(message);
	}

	public NoSuchJobTypeException(Throwable cause) {
		super(cause);
	}

}
