package libconnect.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import libconnect.integration.BookDetails;
import libconnect.integration.BookManagement;
import libconnect.integration.BookSummary;

/** Provides clearly temporary in-memory book data for manual CLI testing. */
public final class DemoBookManagement implements BookManagement {
    private final Map<String, BookSummary> books = new LinkedHashMap<>();

    /** Creates demo book data with one unavailable book for reservation tests. */
    /** Creates the temporary catalogue used by manual librarian verification. */
    public DemoBookManagement() {
        BookDetails details = new BookDetails("978-demo", "Java Fundamentals", "Demo Author",
                "LibConnect Press", "Programming", 2025);
        books.put("b1", new BookSummary("b1", details, 0));
    }

    /** Returns whether a demo book exists. */
    @Override
    public boolean exists(String bookId) {
        return books.containsKey(bookId);
    }

    /** Returns whether a demo book has an available copy. */
    @Override
    public boolean isAvailable(String bookId) {
        return getRequired(bookId).availableCopies() > 0;
    }

    /** Adds a demo book using its ISBN as the stable identifier. */
    @Override
    public String addBook(BookDetails details) {
        String bookId = details.isbn();
        if (books.containsKey(bookId)) {
            throw new IllegalStateException("Book already exists");
        }
        books.put(bookId, new BookSummary(bookId, details, 0));
        return bookId;
    }

    /** Edits a demo book while preserving its stable identifier. */
    @Override
    public void editBook(String bookId, BookDetails details) {
        BookSummary current = getRequired(bookId);
        books.put(bookId, new BookSummary(bookId, details, current.availableCopies()));
    }

    /** Removes a demo book. */
    @Override
    public void removeBook(String bookId) {
        if (books.remove(bookId) == null) {
            throw new IllegalArgumentException("Book does not exist");
        }
    }

    /** Searches demo books by identifier or catalogue fields. */
    @Override
    public List<BookSummary> searchBooks(String query) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return books.values().stream()
                .filter(book -> normalizedQuery.isBlank()
                        || book.bookId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || book.details().title().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || book.details().author().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || book.details().category().toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .toList();
    }

    private BookSummary getRequired(String bookId) {
        BookSummary book = books.get(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book does not exist");
        }
        return book;
    }
}
