package uniolunisaar.adam.modelchecker.exceptions;

/**
 *
 * @author Manuel Gieseking
 */
public class ExternalToolException extends Exception {

    private static final long serialVersionUID = 1L;

    public ExternalToolException() {
    }

    public ExternalToolException(String message) {
        super(message);
    }

    public ExternalToolException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalToolException(Throwable cause) {
        super(cause);
    }

    public ExternalToolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
