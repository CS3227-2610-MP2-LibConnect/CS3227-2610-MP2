package libconnect.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import libconnect.util.ValidationUtils;

/**
 * Represents the metadata for a book in the library catalogue.
 */
public class Book implements libconnect.storage.repositories.Identifiable {
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
    @JsonCreator
    public Book(@JsonProperty("isbn") String isbn,
                @JsonProperty("title") String title,
                @JsonProperty("author") String author,
                @JsonProperty("publisher") String publisher,
                @JsonProperty("category") String category,
                @JsonProperty("publicationYear") int publicationYear) {
        this.isbn = ValidationUtils.requireNonBlank(isbn, "isbn");
        if (publicationYear <= 0) {
            throw new IllegalArgumentException("publicationYear must be positive");
        }
        this.title = ValidationUtils.requireNonBlank(title, "title");
        this.author = ValidationUtils.requireNonBlank(author, "author");
        this.publisher = ValidationUtils.requireNonBlank(publisher, "publisher");
        this.category = ValidationUtils.requireNonBlank(category, "category");
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

    @Override 
    public String getId() {
        return isbn;
    }

}
