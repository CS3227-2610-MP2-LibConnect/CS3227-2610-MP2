package libconnect.services;

/** Represents the optional criteria used when searching the book catalogue. */
public final class BookSearchCriteria {
    private final String isbn;
    private final String title;
    private final String author;
    private final String publisher;
    private final String category;
    private final Integer publicationYearStart;
    private final Integer publicationYearEnd;

    /**
     * Creates a search criteria object.
     *
     * @param isbn the exact ISBN to match, ignoring case, or null to ignore ISBN.
     * @param title the title text to match, or null to ignore title.
     * @param author the author text to match, or null to ignore author.
     * @param publisher the publisher text to match, or null to ignore publisher.
     * @param category the category text to match, or null to ignore category.
     * @param publicationYearStart the first publication year in the range, or null to ignore year.
     * @param publicationYearEnd the last publication year in the range, or null to ignore year.
     */
    public BookSearchCriteria(String isbn, String title, String author, String publisher,
                              String category, Integer publicationYearStart, Integer publicationYearEnd) {
        this.isbn = normalizeText(isbn);
        this.title = normalizeText(title);
        this.author = normalizeText(author);
        this.publisher = normalizeText(publisher);
        this.category = normalizeText(category);
        this.publicationYearStart = normalizeYear(publicationYearStart, "publicationYearStart");
        this.publicationYearEnd = normalizeYear(publicationYearEnd, "publicationYearEnd");
        if (this.publicationYearStart != null && this.publicationYearEnd != null
                && this.publicationYearStart > this.publicationYearEnd) {
            throw new IllegalArgumentException("publicationYearStart must not be greater than publicationYearEnd");
        }
    }

    /**
     * Returns the exact ISBN criterion, ignoring case, or null when it is not required.
     *
     * @return the ISBN criterion.
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Returns the title criterion, or null when it is not required.
     *
     * @return the title criterion.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the author criterion, or null when it is not required.
     *
     * @return the author criterion.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Returns the publisher criterion, or null when it is not required.
     *
     * @return the publisher criterion.
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Returns the category criterion, or null when it is not required.
     *
     * @return the category criterion.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the first publication year criterion, or null when it is not required.
     *
     * @return the first publication year criterion.
     */
    public Integer getPublicationYearStart() {
        return publicationYearStart;
    }

    /**
     * Returns the last publication year criterion, or null when it is not required.
     *
     * @return the last publication year criterion.
     */
    public Integer getPublicationYearEnd() {
        return publicationYearEnd;
    }

    /**
     * Returns whether this criteria object contains no active filters.
     *
     * @return true if all criteria are empty.
     */
    public boolean isEmpty() {
        return isbn == null && title == null && author == null && publisher == null && category == null
                && publicationYearStart == null && publicationYearEnd == null;
    }

    /**
     * Trims text criteria and converts blank values to null.
     *
     * @param value the text criterion to normalize.
     * @return the normalized criterion, or null when it is blank.
     */
    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Validates and normalizes an optional publication year.
     *
     * @param value the year to normalize.
     * @param fieldName the name of the year field.
     * @return the supplied year, or null when the year is omitted.
     * @throws IllegalArgumentException if the year is not positive.
     */
    private static Integer normalizeYear(Integer value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
