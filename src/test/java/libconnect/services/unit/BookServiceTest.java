package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.services.BookCopyService;
import libconnect.services.BookSearchCriteria;
import libconnect.services.BookService;
import libconnect.services.NotFoundException;
import libconnect.services.ServiceException;
import libconnect.storage.file.FileBookCopyRepository;
import libconnect.storage.file.FileBookRepository;

/** Tests catalogue behavior implemented by {@link BookService}. */
class BookServiceTest {
    @TempDir
    Path temporaryDirectory;

    private FileBookRepository bookRepository;
    private FileBookCopyRepository copyRepository;
    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookRepository = new FileBookRepository(temporaryDirectory.resolve("books.json"));
        copyRepository = new FileBookCopyRepository(temporaryDirectory.resolve("copies.json"));
        bookService = new BookService(bookRepository, new BookCopyService(copyRepository));
    }

    @Test
    void getBooksByCategory_caseInsensitiveAndTrimmed_returnsMatchingBooks() {
        Book firstBook = book("978-1", "Algorithms", "Grace Hopper", "Tech Press", "Technology", 2020);
        Book secondBook = book("978-2", "Poems", "Ada Lovelace", "Literary Press", "Literature", 1815);
        bookRepository.save(firstBook);
        bookRepository.save(secondBook);

        assertEquals(List.of(firstBook), bookService.getBooksByCategory(" technology "));
    }

    @Test
    void getBooksByPublicationYear_inclusiveRange_returnsBoundaryBooks() {
        Book before = book("978-1", "Before", "Author", "Publisher", "Category", 1999);
        Book start = book("978-2", "Start", "Author", "Publisher", "Category", 2000);
        Book withinLow = book("978-3", "Within Low", "Author", "Publisher", "Category", 2001);
        Book withinHigh = book("978-4", "Within High", "Author", "Publisher", "Category", 2019);
        Book end = book("978-5", "End", "Author", "Publisher", "Category", 2020);
        Book after = book("978-6", "After", "Author", "Publisher", "Category", 2021);
        bookRepository.save(before);
        bookRepository.save(start);
        bookRepository.save(withinLow);
        bookRepository.save(withinHigh);
        bookRepository.save(end);
        bookRepository.save(after);

        assertEquals(List.of(start, withinLow, withinHigh, end),
                bookService.getBooksByPublicationYear(2000, 2020));
    }

    @Test
    void getBooksByPublicationYear_reversedRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.getBooksByPublicationYear(2021, 2020));
    }

    @Test
    void getBooksByPublicationYear_singleYear_returnsExactMatches() {
        Book matchingBook = book("978-1", "Matching", "Author", "Publisher", "Category", 2020);
        bookRepository.save(matchingBook);
        bookRepository.save(book("978-2", "Other", "Author", "Publisher", "Category", 2021));

        assertEquals(List.of(matchingBook), bookService.getBooksByPublicationYear(2020));
    }

    @Test
    void getBookByIsbnAndGetAllBooks_returnPersistedBooks() {
        Book firstBook = book("978-1", "First", "Author", "Publisher", "Category", 2020);
        Book secondBook = book("978-2", "Second", "Author", "Publisher", "Category", 2021);
        bookRepository.save(firstBook);
        bookRepository.save(secondBook);

        assertEquals(firstBook, bookService.getBookByIsbn("978-1").orElseThrow());
        assertTrue(bookService.getBookByIsbn("978-unknown").isEmpty());
        assertEquals(List.of(firstBook, secondBook), bookService.getAllBooks());
    }

    @Test
    void getBooksByTitleAndAuthor_delegatesCaseInsensitiveSearch() {
        Book expectedBook = book("978-1", "The Great Adventure", "Mary Shelley", "Publisher",
                "Category", 1818);
        bookRepository.save(expectedBook);

        assertEquals(List.of(expectedBook), bookService.getBooksByTitle(" GREAT "));
        assertEquals(List.of(expectedBook), bookService.getBooksByAuthor(" shelley "));
    }

    @Test
    void createBook_duplicateIsbn_throwsAndKeepsOriginal() {
        Book originalBook = book("978-1", "Original", "Author", "Publisher", "Category", 2020);
        bookRepository.save(originalBook);

        assertThrows(ServiceException.class, () -> bookService.createBook("978-1", "Replacement", "Author 2",
                "Publisher 2", "Category 2", 2021));
        assertEquals(originalBook, bookService.getBookByIsbn("978-1").orElseThrow());
    }

    @Test
    void createBook_persistsCatalogueMetadata() {
        assertDoesNotThrow(() -> bookService.createBook("978-1", "Original", "Author", "Publisher",
                "Category", 2020));

        Book createdBook = bookService.getBookByIsbn("978-1").orElseThrow();
        assertEquals("Original", createdBook.getTitle());
        assertEquals("Author", createdBook.getAuthor());
        assertEquals("Publisher", createdBook.getPublisher());
        assertEquals("Category", createdBook.getCategory());
        assertEquals(2020, createdBook.getPublicationYear());
    }

    @Test
    void updateBook_persistsCatalogueMetadata() {
        bookService.createBook("978-1", "Original", "Author", "Publisher", "Category", 2020);

        bookService.updateBook("978-1", "Updated", "New Author", "New Publisher",
                "New Category", 2021);

        Book updatedBook = bookService.getBookByIsbn("978-1").orElseThrow();
        assertEquals("Updated", updatedBook.getTitle());
        assertEquals("New Author", updatedBook.getAuthor());
        assertEquals("New Publisher", updatedBook.getPublisher());
        assertEquals("New Category", updatedBook.getCategory());
        assertEquals(2021, updatedBook.getPublicationYear());
    }

    @Test
    void updateBook_unknownIsbn_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> bookService.updateBook("978-unknown", "Title", "Author", "Publisher",
                        "Category", 2020));
    }

    @Test
    void deleteBook_removesBookAndAssociatedCopies() {
        bookRepository.save(book("978-1", "Title", "Author", "Publisher", "Category", 2020));
        copyRepository.save(new BookCopy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A-1"));
        copyRepository.save(new BookCopy("COPY-2", "978-2", CopyStatus.AVAILABLE, "A-2"));

        bookService.deleteBook("978-1");

        assertTrue(bookService.getBookByIsbn("978-1").isEmpty());
        assertTrue(copyRepository.findByIsbn("978-1").isEmpty());
        assertTrue(copyRepository.findById("COPY-2").isPresent());
    }

    @Test
    void getBooksByCategory_blankCategory_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> bookService.getBooksByCategory("   "));
    }

    @Test
    void searchBooks_combinesCaseInsensitiveTextAndYearCriteria() {
        Book expectedBook = book("978-1", "The Great Adventure", "Mary Shelley", "Tech Press",
                "Technology", 2020);
        bookRepository.save(expectedBook);
        bookRepository.save(book("978-2", "The Great Adventure", "Mary Shelley", "Tech Press",
                "Technology", 2021));

        BookSearchCriteria criteria = new BookSearchCriteria(" 978-1 ", " great ", " shelley ",
                " tech ", " technology ", 2020, 2020);

        assertEquals(List.of(expectedBook), bookService.searchBooks(criteria));
    }

    @Test
    void searchBooks_nullCriteria_rejected() {
        assertThrows(NullPointerException.class, () -> bookService.searchBooks(null));
    }

    @Test
    void optionalPublicationYearFilters_supportOpenBounds() {
        Book olderBook = book("978-1", "Older", "Author", "Publisher", "Category", 2000);
        Book newerBook = book("978-2", "Newer", "Author", "Publisher", "Category", 2020);
        bookRepository.save(olderBook);
        bookRepository.save(newerBook);

        assertEquals(List.of(newerBook), bookService.filterBooksByPublicationYearRange(
                List.of(olderBook, newerBook), 2010, null));
        assertEquals(List.of(olderBook), bookService.filterBooksByPublicationYearRange(
                List.of(olderBook, newerBook), null, 2010));
        assertEquals(List.of(olderBook, newerBook), bookService.filterBooksByPublicationYearRange(
                List.of(olderBook, newerBook), null, null));
    }

    @Test
    void filterBooks_blankCriteria_returnOriginalList() {
        List<Book> books = List.of(book("978-1", "Title", "Author", "Publisher", "Category", 2020));

        assertEquals(books, bookService.filterBooksByCategory(books, "   "));
        assertEquals(books, bookService.filterBooksByAuthor(books, null));
        assertEquals(books, bookService.filterBooksByTitle(books, ""));
        assertEquals(books, bookService.filterBooksByIsbn(books, "   "));
        assertEquals(books, bookService.filterBooksByPublisher(books, null));
    }

    @Test
    void filterBooks_publisherAndIsbnUseExpectedMatchingRules() {
        Book firstBook = book("978-1", "Title", "Author", "Tech Press", "Category", 2020);
        Book secondBook = book("978-10", "Title", "Author", "Other Press", "Category", 2020);
        List<Book> books = List.of(firstBook, secondBook);

        assertEquals(List.of(firstBook), bookService.filterBooksByPublisher(books, " tech "));
        assertEquals(List.of(firstBook), bookService.filterBooksByIsbn(books, " 978-1 "));
    }

    @Test
    void bookCreationAndUpdate_invalidFields_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> bookService.createBook(" ", "Title", "Author", "Publisher", "Category", 2020));
        assertThrows(IllegalArgumentException.class,
                () -> bookService.createBook("978-1", " ", "Author", "Publisher", "Category", 2020));
        bookService.createBook("978-1", "Title", "Author", "Publisher", "Category", 2020);
        assertThrows(IllegalArgumentException.class,
                () -> bookService.updateBook("978-1", " ", "Author", "Publisher", "Category", 2020));
    }

    @Test
    void filterBooks_invalidPublicationYears_rejected() {
        List<Book> books = List.of(book("978-1", "Title", "Author", "Publisher", "Category", 2020));

        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooksByPublicationYear(books, null));
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooksByPublicationYearRange(books, Integer.valueOf(0), Integer.valueOf(2020)));
        assertThrows(IllegalArgumentException.class,
                () -> bookService.filterBooksByPublicationYearRange(books, Integer.valueOf(2021),
                        Integer.valueOf(2020)));
    }

    @Test
    void deleteBook_unknownIsbn_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () -> bookService.deleteBook("978-unknown"));
    }

    private static Book book(String isbn, String title, String author, String publisher,
                             String category, int publicationYear) {
        return new Book(isbn, title, author, publisher, category, publicationYear);
    }
}
