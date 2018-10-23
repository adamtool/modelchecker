package uniolunisaar.adam.modelchecker.exceptions;

import uniolunisaar.adam.ds.exceptions.SolvingException;

/**
 *
 * @author Manuel Gieseking
 */
public class NotConvertableException extends SolvingException {

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
