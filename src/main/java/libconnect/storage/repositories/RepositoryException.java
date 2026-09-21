package libconnect.storage.repositories;

/** Signals a persistence failure that prevents an operation from succeeding. */
public class RepositoryException extends RuntimeException {
    /** Creates a persistence exception with a message and cause. */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
