package libconnect.integration;

/** Exposes the minimum book information required by librarian screens. */
public record BookSummary(String bookId, BookDetails details, int availableCopies) {
    /** Creates a book summary and rejects impossible availability. */
    public BookSummary {
        if (bookId == null || bookId.isBlank()) {
            throw new IllegalArgumentException("bookId must not be blank");
        }
        if (details == null) {
            throw new IllegalArgumentException("details must not be null");
        }
        if (availableCopies < 0) {
            throw new IllegalArgumentException("availableCopies must not be negative");
        }
    }
}
