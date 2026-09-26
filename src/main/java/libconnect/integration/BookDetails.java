package libconnect.integration;

/** Defines the book fields required by librarian catalogue management. */
public record BookDetails(String isbn, String title, String author, String publisher,
                          String category, int publicationYear) {
    /** Creates book details and rejects missing core catalogue fields. */
    public BookDetails {
        requireText(isbn, "isbn");
        requireText(title, "title");
        requireText(author, "author");
        requireText(publisher, "publisher");
        requireText(category, "category");
        if (publicationYear <= 0) {
            throw new IllegalArgumentException("publicationYear must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
