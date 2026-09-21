package models;

/**
 * Represents the metadata for a book in the library catalogue.
 */
public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final String category;
    private final int publicationYear;

    /**
     * Creates a book with the supplied catalogue metadata.
     *
     * @param isbn the stable ISBN identifying the book.
     * @param title the book title.
     * @param author the book author.
     * @param publisher the book publisher.
     * @param category the book category.
     * @param publicationYear the year in which the book was published.
     * @throws IllegalArgumentException if a text value is blank or the publication year is invalid.
     */
    public Book(String isbn, String title, String author, String publisher,
                String category, int publicationYear) {
        this.isbn = requireNonBlank(isbn, "isbn");
        if (publicationYear <= 0) {
            throw new IllegalArgumentException("publicationYear must be positive");
        }
        this.title = requireNonBlank(title, "title");
        this.author = requireNonBlank(author, "author");
        this.publisher = requireNonBlank(publisher, "publisher");
        this.category = requireNonBlank(category, "category");
        this.publicationYear = publicationYear;
    }

    /**
     * Returns the ISBN identifying this book.
     *
     * @return the book ISBN.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Returns the book title.
     *
     * @return the book title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the book author.
     *
     * @return the book author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the book publisher.
     *
     * @return the book publisher.
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Returns the book category.
     *
     * @return the book category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the book publication year.
     *
     * @return the publication year.
     */
    public int getPublicationYear() {
        return publicationYear;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Book otherBook)) {
            return false;
        }
        return isbn.equals(otherBook.isbn);
    }

    @Override
    public String toString() {
        return "Book{" + "isbn='" + isbn + '\'' + ", title='" + title + '\''
                + ", author='" + author + '\'' + ", publisher='" + publisher + '\''
                + ", category='" + category + '\'' + ", publicationYear=" + publicationYear + '}';
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
