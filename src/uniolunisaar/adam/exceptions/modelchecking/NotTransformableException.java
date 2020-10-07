package uniolunisaar.adam.exceptions.modelchecking;

/**
 *
 * @author Manuel Gieseking
 */
public class NotTransformableException extends Exception {

    private static final long serialVersionUID = 1L;

    public NotTransformableException() {
    }

    public NotTransformableException(String message) {
        super(message);
    }

    public NotTransformableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotTransformableException(Throwable cause) {
        super(cause);
    }

    public NotTransformableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
