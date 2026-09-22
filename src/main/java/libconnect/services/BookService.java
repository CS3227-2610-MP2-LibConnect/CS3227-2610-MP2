package libconnect.services;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import libconnect.models.Book;
import libconnect.storage.file.FileBookRepository;
import libconnect.util.ValidationUtils;

/** Provides catalogue book operations through file-backed repositories. */
public class BookService {
    private final FileBookRepository bookRepository;
    private final BookCopyService bookCopyService;

    /** Creates a service backed by the default book and book-copy data files. */
    public BookService() {
        this(new FileBookRepository(), new BookCopyService());
    }

    /**
     * Creates a service backed by the supplied repositories and copy service.
     *
     * @param bookRepository the repository used to persist books.
     * @param bookCopyService the service used to delete copies associated with a book.
     * @throws NullPointerException if either argument is null.
     */
    public BookService(FileBookRepository bookRepository, BookCopyService bookCopyService) {
        this.bookRepository = Objects.requireNonNull(bookRepository, "bookRepository");
        this.bookCopyService = Objects.requireNonNull(bookCopyService, "bookCopyService");
    }

    /**
     * Returns all books in the supplied category.
     *
     * <p>Category matching is case-insensitive and ignores surrounding whitespace.</p>
     *
     * @param category the category to search for.
     * @return all books in the category.
     * @throws IllegalArgumentException if {@code category} is blank.
     */
    public List<Book> getBooksByCategory(String category) {
        String requiredCategory = ValidationUtils.requireNonBlank(category, "category");
        return bookRepository.findAll().stream()
                .filter(book -> book.getCategory().equalsIgnoreCase(requiredCategory))
                .toList();
    }

    /**
     * Returns books whose authors contain the supplied text.
     *
     * @param author the author text to search for.
     * @return all books with a matching author.
     * @throws IllegalArgumentException if {@code author} is blank.
     */
    public List<Book> getBooksByAuthor(String author) {
        return bookRepository.findByAuthor(author);
    }

    /**
     * Returns books published within an inclusive year range.
     *
     * @param startYear the first publication year in the range.
     * @param endYear the last publication year in the range.
     * @return all books published between the supplied years, inclusive.
     * @throws IllegalArgumentException if {@code startYear} is greater than {@code endYear}.
     */
    public List<Book> getBooksByPublicationYear(int startYear, int endYear) {
        if (startYear > endYear) {
            throw new IllegalArgumentException("startYear must not be greater than endYear");
        }

        return bookRepository.findAll().stream()
                .filter(book -> book.getPublicationYear() >= startYear
                        && book.getPublicationYear() <= endYear)
                .toList();
    }

    /**
     * Returns books published in a specific year.
     *
     * @param publicationYear the publication year to search for.
     * @return all books published in the specified year.
     * @throws IllegalArgumentException if {@code publicationYear} is not a valid year.
     */
    public List<Book> getBooksByPublicationYear(int publicationYear) {
        return bookRepository.findAll().stream()
                .filter(book -> book.getPublicationYear() == publicationYear)
                .toList();
    }

    /**
     * Returns books whose titles contain the supplied text.
     *
     * @param title the title text to search for.
     * @return all books with a matching title.
     * @throws IllegalArgumentException if {@code title} is blank.
     */
    public List<Book> getBooksByTitle(String title) {
        return bookRepository.findByTitle(title);
    }

    /**
     * Returns a book by its ISBN.
     *
     * @param isbn the ISBN to search for.
     * @return the matching book, or an empty optional if it does not exist.
     * @throws IllegalArgumentException if {@code isbn} is blank.
     */
    public Optional<Book> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn);
    }

    /**
     * Returns all persisted books.
     *
     * @return all books in the catalogue.
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * Returns books matching every non-empty supplied criterion.
     *
     * <p>Text criteria use case-insensitive partial matching. ISBN uses exact case-insensitive
     * matching, while publication year uses inclusive range matching.</p>
     *
     * @param criteria the optional criteria to apply.
     * @return all books matching the supplied criteria.
     * @throws NullPointerException if {@code criteria} is null.
     */
    public List<Book> searchBooks(BookSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "criteria cannot be null");
        List<Book> books = getAllBooks();
        books = filterBooksByIsbn(books, criteria.getIsbn());
        books = filterBooksByTitle(books, criteria.getTitle());
        books = filterBooksByAuthor(books, criteria.getAuthor());
        books = filterBooksByPublisher(books, criteria.getPublisher());
        books = filterBooksByCategory(books, criteria.getCategory());
        books = filterBooksByPublicationYearRange(books, criteria.getPublicationYearStart(),
                criteria.getPublicationYearEnd());
        return books;
    }

    // Filters
    /**
     * Returns books in the specified category, or all supplied books when the category is blank.
     *
     * Category matching is case-insensitive.
     *
     * @param books the books to filter.
     * @param category the category to match.
     * @return books matching the category, or {@code books} if {@code category} is null or blank.
     */
    public List<Book> filterBooksByCategory(List<Book> books, String category) {
        if (category == null || category.trim().isEmpty()) {
            return books;
        }
        String requiredCategory = category.trim();
        return books.stream()
                .filter(book -> book.getCategory().equalsIgnoreCase(requiredCategory))
                .toList();
    }

    /**
     * Returns books whose authors contain the specified text, or all supplied books when the text is blank.
     *
     * Author matching is case-insensitive.
     *
     * @param books the books to filter.
     * @param author the author text to match.
     * @return books matching the author text, or {@code books} if {@code author} is null or blank.
     */
    public List<Book> filterBooksByAuthor(List<Book> books, String author) {
        if (author == null || author.trim().isEmpty()) {
            return books;
        }
        String requiredAuthor = author.trim();
        return books.stream()
                .filter(book -> containsIgnoreCase(book.getAuthor(), requiredAuthor))
                .toList();
    }

    /**
     * Returns books published in the specified year.
     *
     * @param books the books to filter.
     * @param publicationYear the publication year to match.
     * @return books published in {@code publicationYear}.
     * @throws IllegalArgumentException if {@code publicationYear} is not positive.
     */
    public List<Book> filterBooksByPublicationYear(List<Book> books, Integer publicationYear) {
        if (publicationYear == null || publicationYear <= 0) {
            throw new IllegalArgumentException("publicationYear must be positive");
        }
        return books.stream()
                .filter(book -> book.getPublicationYear() == publicationYear)
                .toList();
    }

    /**
     * Returns books published within the specified inclusive year range.
     *
     * @param books the books to filter.
     * @param startYear the first publication year in the range.
     * @param endYear the last publication year in the range.
     * @return books published between {@code startYear} and {@code endYear}, inclusive.
     * @throws IllegalArgumentException if {@code startYear} is greater than {@code endYear}.
     */
    public List<Book> filterBooksByPublicationYearRange(List<Book> books, int startYear, int endYear) {
        if (startYear > endYear) {
            throw new IllegalArgumentException("startYear must not be greater than endYear");
        }
        return books.stream()
                .filter(book -> book.getPublicationYear() >= startYear
                        && book.getPublicationYear() <= endYear)
                .toList();
    }

    /**
     * Returns books published within the specified optional inclusive year range.
     *
     * @param books the books to filter.
     * @param startYear the first publication year, or null for no lower bound.
     * @param endYear the last publication year, or null for no upper bound.
     * @return books matching the supplied optional range.
     * @throws IllegalArgumentException if a supplied year is not positive or the range is reversed.
     */
    public List<Book> filterBooksByPublicationYearRange(List<Book> books, Integer startYear, Integer endYear) {
        if (startYear != null && startYear <= 0) {
            throw new IllegalArgumentException("startYear must be positive");
        }
        if (endYear != null && endYear <= 0) {
            throw new IllegalArgumentException("endYear must be positive");
        }
        if (startYear != null && endYear != null && startYear > endYear) {
            throw new IllegalArgumentException("startYear must not be greater than endYear");
        }
        if (startYear == null && endYear == null) {
            return books;
        }

        return books.stream()
                .filter(book -> (startYear == null || book.getPublicationYear() >= startYear)
                        && (endYear == null || book.getPublicationYear() <= endYear))
                .toList();
    }

    /**
     * Returns books whose titles contain the specified text, or all supplied books when the text is blank.
     *
     * Title matching is case-insensitive.
     *
     * @param books the books to filter.
     * @param title the title text to match.
     * @return books matching the title text, or {@code books} if {@code title} is null or blank.
     */
    public List<Book> filterBooksByTitle(List<Book> books, String title) {
        if (title == null || title.trim().isEmpty()) {
            return books;
        }
        String requiredTitle = title.trim();
        return books.stream()
                .filter(book -> containsIgnoreCase(book.getTitle(), requiredTitle))
                .toList();
    }

    /**
     * Returns books whose ISBN exactly matches the supplied value, ignoring case.
     *
     * @param books the books to filter.
     * @param isbn the ISBN to match.
     * @return books matching the ISBN, or {@code books} if the ISBN is blank.
     */
    public List<Book> filterBooksByIsbn(List<Book> books, String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return books;
        }
        String requiredIsbn = isbn.trim();
        return books.stream()
                .filter(book -> book.getIsbn().equalsIgnoreCase(requiredIsbn))
                .toList();
    }

    /**
     * Returns books whose publishers contain the specified text, or all supplied books when the text is blank.
     *
     * Publisher matching is case-insensitive.
     *
     * @param books the books to filter.
     * @param publisher the publisher text to match.
     * @return books matching the publisher text, or {@code books} if {@code publisher} is null or blank.
     */
    public List<Book> filterBooksByPublisher(List<Book> books, String publisher) {
        if (publisher == null || publisher.trim().isEmpty()) {
            return books;
        }
        String requiredPublisher = publisher.trim();
        return books.stream()
                .filter(book -> containsIgnoreCase(book.getPublisher(), requiredPublisher))
                .toList();
    }

    /**
     * Returns whether a value contains a query, ignoring case.
     *
     * @param value the value to search.
     * @param query the query to find.
     * @return true if the query occurs in the value.
     */
    private boolean containsIgnoreCase(String value, String query) {
        return value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    /**
     * Creates and persists a new book.
     *
     * @param isbn the ISBN of the new book.
     * @param title the title of the new book.
     * @param author the author of the new book.
     * @param publisher the publisher of the new book.
     * @param category the category of the new book.
     * @param publicationYear the publication year of the new book.
     * @return true if the book was created, or false if the ISBN already exists.
     */
    public void createBook(String isbn, String title, String author, String publisher,
                              String category, int publicationYear) {
        if (bookRepository.findByIsbn(isbn).isPresent()) {
            throw new ServiceException("Book with ISBN already exists: " + isbn);
        }

        bookRepository.save(new Book(isbn, title, author, publisher, category, publicationYear));
    }

    /**
     * Updates an existing book and persists its new catalogue metadata.
     *
     * @param isbn the ISBN of the book to update.
     * @param title the updated title.
     * @param author the updated author.
     * @param publisher the updated publisher.
     * @param category the updated category.
     * @param publicationYear the updated publication year.
     * @throws NotFoundException if no book has the supplied ISBN.
     */
    public void updateBook(String isbn, String title, String author, String publisher,
                           String category, int publicationYear) {
        findBook(isbn);
        bookRepository.save(new Book(isbn, title, author, publisher, category, publicationYear));
    }

    /**
     * Deletes a book and all copies associated with its ISBN.
     *
     * @param isbn the ISBN of the book to delete.
     * @throws NotFoundException if no book has the supplied ISBN.
     */
    public void deleteBook(String isbn) {
        findBook(isbn);
        bookCopyService.deleteCopiesByIsbn(isbn);
        if (!bookRepository.deleteByIsbn(isbn)) {
            throw new NotFoundException("Book not found: " + isbn);
        }
    }

    private Book findBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new NotFoundException("Book not found: " + isbn));
    }
}
