package au.org.massive.strudel_web.job_control;

/**
 * Thrown if a job is attempted without the required parameters
 * 
 * @author jrigby
 *
 */
public class MissingRequiredJobParametersException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8194220157009834131L;

	public MissingRequiredJobParametersException() {
		super();
	}

	public MissingRequiredJobParametersException(String message,
			Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public MissingRequiredJobParametersException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingRequiredJobParametersException(String message) {
		super(message);
	}

	public MissingRequiredJobParametersException(Throwable cause) {
		super(cause);
	}

}
