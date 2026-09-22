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
    void getBooksByTitleAndAuthor_delegatesCaseInsensitiveSearch() {
        Book expectedBook = book("978-1", "The Great Adventure", "Mary Shelley", "Publisher",
                "Category", 1818);
        bookRepository.save(expectedBook);

        assertEquals(List.of(expectedBook), bookService.getBooksByTitle(" GREAT "));
        assertEquals(List.of(expectedBook), bookService.getBooksByAuthor(" shelley "));
    }

    @Test
    void createBook_duplicateIsbn_returnsFalseAndKeepsOriginal() {
        Book originalBook = book("978-1", "Original", "Author", "Publisher", "Category", 2020);
        bookRepository.save(originalBook);

        assertThrows(ServiceException.class, () -> bookService.createBook("978-1", "Replacement", "Author 2", "Publisher 2",
                "Category 2", 2021));
        assertEquals(originalBook, bookService.getBookByIsbn("978-1").orElseThrow());
    }

    @Test
    void createAndUpdateBook_persistsCatalogueMetadata() {
        assertDoesNotThrow(() -> bookService.createBook("978-1", "Original", "Author", "Publisher",
                "Category", 2020));

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

    private static Book book(String isbn, String title, String author, String publisher,
                             String category, int publicationYear) {
        return new Book(isbn, title, author, publisher, category, publicationYear);
    }
}
