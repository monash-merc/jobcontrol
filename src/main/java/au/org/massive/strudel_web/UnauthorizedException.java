package au.org.massive.strudel_web;

/**
 * Thrown during the OAuth2 flow if the user is not authorised.
 *
 * @author jrigby
 */
public class UnauthorizedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -4562151977714415465L;

    public UnauthorizedException() {
        super();
    }

    public UnauthorizedException(String message, Throwable cause,
                                 boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }

}
