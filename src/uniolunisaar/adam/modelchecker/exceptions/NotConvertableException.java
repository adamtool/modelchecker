package uniolunisaar.adam.modelchecker.exceptions;

/**
 *
 * @author Manuel Gieseking
 */
public class NotConvertableException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotConvertableException() {
    }

    public NotConvertableException(String message) {
        super(message);
    }

    public NotConvertableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotConvertableException(Throwable cause) {
        super(cause);
    }

    public NotConvertableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
