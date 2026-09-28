package libconnect.models;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

class BookTest {
    @Test
    void constructor_validValues_returnsMetadata() {
        Book book = new Book("978-1", "Title", "Author", "Publisher",
                "Category", 2025);

        assertAll(
                () -> assertEquals("978-1", book.getIsbn()),
                () -> assertEquals("Title", book.getTitle()),
                () -> assertEquals("Author", book.getAuthor()),
                () -> assertEquals("Publisher", book.getPublisher()),
                () -> assertEquals("Category", book.getCategory()),
                () -> assertEquals(2025, book.getPublicationYear()));
    }

    @Test
    void constructor_surroundingWhitespace_trimsTextValues() {
        Book book = new Book(" 978-1 ", " Title ", " Author ", " Publisher ",
                " Category ", 2025);

        assertAll(
                () -> assertEquals("978-1", book.getIsbn()),
                () -> assertEquals("Title", book.getTitle()),
                () -> assertEquals("Author", book.getAuthor()),
                () -> assertEquals("Publisher", book.getPublisher()),
                () -> assertEquals("Category", book.getCategory()));
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void constructor_invalidTextValue_throwsIllegalArgumentException(String isbn, String title,
            String author, String publisher, String category) {
        assertThrows(IllegalArgumentException.class,
                () -> new Book(isbn, title, author, publisher, category, 2025));
    }

    private static Stream<Arguments> invalidTextValues() {
        return Stream.of(
                Arguments.of(null, "Title", "Author", "Publisher", "Category"),
                Arguments.of("", "Title", "Author", "Publisher", "Category"),
                Arguments.of("   ", "Title", "Author", "Publisher", "Category"),
                Arguments.of("978-1", null, "Author", "Publisher", "Category"),
                Arguments.of("978-1", "", "Author", "Publisher", "Category"),
                Arguments.of("978-1", "   ", "Author", "Publisher", "Category"),
                Arguments.of("978-1", "Title", null, "Publisher", "Category"),
                Arguments.of("978-1", "Title", "", "Publisher", "Category"),
                Arguments.of("978-1", "Title", "   ", "Publisher", "Category"),
                Arguments.of("978-1", "Title", "Author", null, "Category"),
                Arguments.of("978-1", "Title", "Author", "", "Category"),
                Arguments.of("978-1", "Title", "Author", "   ", "Category"),
                Arguments.of("978-1", "Title", "Author", "Publisher", null),
                Arguments.of("978-1", "Title", "Author", "Publisher", ""),
                Arguments.of("978-1", "Title", "Author", "Publisher", "   "));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void constructor_nonPositivePublicationYear_throwsIllegalArgumentException(int publicationYear) {
        assertThrows(IllegalArgumentException.class,
                () -> new Book("978-1", "Title", "Author", "Publisher", "Category",
                        publicationYear));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void constructor_smallestPositivePublicationYears_createsBook(int publicationYear) {
        Book book = new Book("978-1", "Title", "Author", "Publisher", "Category",
                publicationYear);

        assertEquals(publicationYear, book.getPublicationYear());
    }

    @Test
    void equals_sameIsbnDifferentMetadata_returnsTrue() {
        Book firstBook = new Book("978-1", "Title", "Author", "Publisher", "Category", 2025);
        Book secondBook = new Book("978-1", "Different title", "Different author",
                "Different publisher", "Different category", 2024);

        assertEquals(firstBook, secondBook);
    }

    @Test
    void equals_differentIsbn_returnsFalse() {
        Book firstBook = new Book("978-1", "Title", "Author", "Publisher", "Category", 2025);
        Book secondBook = new Book("978-2", "Title", "Author", "Publisher", "Category", 2025);

        assertNotEquals(firstBook, secondBook);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        Book book = createBook();

        assertEquals(book, book);
    }

    @Test
    void equals_nullOrDifferentType_returnsFalse() {
        Book book = createBook();

        assertAll(
                () -> assertFalse(book.equals(null)),
                () -> assertFalse(book.equals("not a book")));
    }

    @Test
    void toString_containsBookDetails() {
        Book book = createBook();
        String representation = book.toString();

        assertAll(
                () -> assertTrue(representation.contains("978-1")),
                () -> assertTrue(representation.contains("Title")),
                () -> assertTrue(representation.contains("Author")),
                () -> assertTrue(representation.contains("Publisher")),
                () -> assertTrue(representation.contains("Category")),
                () -> assertTrue(representation.contains("2025")));
    }

    private static Book createBook() {
        return new Book("978-1", "Title", "Author", "Publisher", "Category", 2025);
    }
}
