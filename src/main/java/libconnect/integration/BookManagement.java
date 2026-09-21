package libconnect.integration;

import java.util.List;

/** Defines the catalogue-management boundary owned by the member-role developer. */
public interface BookManagement extends BookCatalogue {
    /** Adds a book and returns its stable identifier. */
    String addBook(BookDetails details);

    /** Edits an existing book. */
    void editBook(String bookId, BookDetails details);

    /** Removes a book when the member-role rules permit removal. */
    void removeBook(String bookId);

    /** Searches books using the shared catalogue implementation. */
    List<BookSummary> searchBooks(String query);
}
