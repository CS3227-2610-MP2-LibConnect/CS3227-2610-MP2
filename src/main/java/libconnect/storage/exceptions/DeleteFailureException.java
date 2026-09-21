package libconnect.storage.exceptions;

/**
 * Signals that a requested deletion could not be completed.
 */
public class DeleteFailureException extends Exception {
    /**
     * Creates a deletion failure with the supplied message.
     *
     * @param message the reason for the deletion failure.
     */
    public DeleteFailureException(String message) {
        super(message);
    }
}
