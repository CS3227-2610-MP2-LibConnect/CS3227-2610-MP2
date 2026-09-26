package libconnect.integration;

/** Provides the minimal book lookup contract required by librarian services. */
public interface BookCatalogue {
    /** Returns whether the book identifier exists. */
    boolean exists(String bookId);

    /** Returns whether the book currently has an available copy. */
    boolean isAvailable(String bookId);
}
