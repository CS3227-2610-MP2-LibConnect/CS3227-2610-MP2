package libconnect.storage.repositories;

/** Signals a persistence failure that prevents a repository operation from succeeding. */
public class RepositoryException extends RuntimeException {
    /**
     * Creates a persistence exception with a message and cause.
     *
     * @param message the detail message describing the persistence failure.
     * @param cause the underlying exception that caused the persistence failure.
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
