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
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class BookCopyTest {
    @Test
    void constructor_withoutStatus_createsAvailableCopy() {
        BookCopy copy = new BookCopy("COPY-1", "978-1", "A1-01");

        assertAll(
                () -> assertEquals("COPY-1", copy.getCopyId()),
                () -> assertEquals("978-1", copy.getIsbn()),
                () -> assertEquals(CopyStatus.AVAILABLE, copy.getStatus()),
                () -> assertEquals("A1-01", copy.getShelfLocation()),
                () -> assertTrue(copy.isAvailable()));
    }

    @ParameterizedTest
    @EnumSource(CopyStatus.class)
    void constructor_withStatus_preservesStatus(CopyStatus status) {
        BookCopy copy = new BookCopy("COPY-1", "978-1", status, "A1-01");

        assertEquals(status, copy.getStatus());
        assertEquals(status == CopyStatus.AVAILABLE, copy.isAvailable());
    }

    @Test
    void constructor_surroundingWhitespace_trimsTextValues() {
        BookCopy copy = new BookCopy(" COPY-1 ", " 978-1 ", " A1-01 ");

        assertAll(
                () -> assertEquals("COPY-1", copy.getCopyId()),
                () -> assertEquals("978-1", copy.getIsbn()),
                () -> assertEquals("A1-01", copy.getShelfLocation()));
    }

    @ParameterizedTest
    @MethodSource("invalidTextValues")
    void constructor_invalidTextValue_throwsIllegalArgumentException(String copyId, String isbn,
            String shelfLocation) {
        assertThrows(IllegalArgumentException.class,
                () -> new BookCopy(copyId, isbn, shelfLocation));
    }

    private static Stream<Arguments> invalidTextValues() {
        return Stream.of(
                Arguments.of(null, "978-1", "A1-01"),
                Arguments.of("", "978-1", "A1-01"),
                Arguments.of("   ", "978-1", "A1-01"),
                Arguments.of("COPY-1", null, "A1-01"),
                Arguments.of("COPY-1", "", "A1-01"),
                Arguments.of("COPY-1", "   ", "A1-01"),
                Arguments.of("COPY-1", "978-1", null),
                Arguments.of("COPY-1", "978-1", ""),
                Arguments.of("COPY-1", "978-1", "   "));
    }

    @Test
    void constructor_nullStatus_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> new BookCopy("COPY-1", "978-1", null, "A1-01"));
    }

    @Test
    void updateShelfLocation_validLocation_updatesLocation() {
        BookCopy copy = createCopy();

        copy.updateShelfLocation(" B2-02 ");

        assertEquals("B2-02", copy.getShelfLocation());
    }

    @ParameterizedTest
    @MethodSource("invalidShelfLocations")
    void updateShelfLocation_invalidLocation_preservesPreviousLocation(String shelfLocation) {
        BookCopy copy = createCopy();

        assertThrows(IllegalArgumentException.class, () -> copy.updateShelfLocation(shelfLocation));
        assertEquals("A1-01", copy.getShelfLocation());
    }

    private static Stream<String> invalidShelfLocations() {
        return Stream.of(null, "", "   ");
    }

    @Test
    void markBorrowed_availableCopy_marksCopyBorrowed() {
        BookCopy copy = createCopy();

        copy.markBorrowed();

        assertAll(
                () -> assertEquals(CopyStatus.BORROWED, copy.getStatus()),
                () -> assertFalse(copy.isAvailable()));
    }

    @ParameterizedTest
    @MethodSource("unavailableStatuses")
    void markBorrowed_unavailableCopy_throwsIllegalStateException(CopyStatus status) {
        BookCopy copy = new BookCopy("COPY-1", "978-1", status, "A1-01");

        assertThrows(IllegalStateException.class, copy::markBorrowed);
        assertEquals(status, copy.getStatus());
    }

    private static Stream<CopyStatus> unavailableStatuses() {
        return Stream.of(CopyStatus.BORROWED, CopyStatus.LOST, CopyStatus.DAMAGED);
    }

    @ParameterizedTest
    @EnumSource(CopyStatus.class)
    void markAvailable_anyStatus_marksCopyAvailable(CopyStatus status) {
        BookCopy copy = new BookCopy("COPY-1", "978-1", status, "A1-01");

        copy.markAvailable();

        assertAll(
                () -> assertEquals(CopyStatus.AVAILABLE, copy.getStatus()),
                () -> assertTrue(copy.isAvailable()));
    }

    @ParameterizedTest
    @EnumSource(CopyStatus.class)
    void markLost_anyStatus_marksCopyLost(CopyStatus status) {
        BookCopy copy = new BookCopy("COPY-1", "978-1", status, "A1-01");

        copy.markLost();

        assertAll(
                () -> assertEquals(CopyStatus.LOST, copy.getStatus()),
                () -> assertFalse(copy.isAvailable()));
    }

    @ParameterizedTest
    @EnumSource(CopyStatus.class)
    void markDamaged_anyStatus_marksCopyDamaged(CopyStatus status) {
        BookCopy copy = new BookCopy("COPY-1", "978-1", status, "A1-01");

        copy.markDamaged();

        assertAll(
                () -> assertEquals(CopyStatus.DAMAGED, copy.getStatus()),
                () -> assertFalse(copy.isAvailable()));
    }

    @Test
    void equals_sameCopyIdDifferentDetails_returnsTrue() {
        BookCopy firstCopy = new BookCopy("COPY-1", "978-1", CopyStatus.AVAILABLE, "A1-01");
        BookCopy secondCopy = new BookCopy("COPY-1", "978-2", CopyStatus.LOST, "B2-02");

        assertEquals(firstCopy, secondCopy);
    }

    @Test
    void equals_differentCopyIds_returnsFalse() {
        BookCopy firstCopy = createCopy();
        BookCopy secondCopy = new BookCopy("COPY-2", "978-1", "A1-01");

        assertNotEquals(firstCopy, secondCopy);
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        BookCopy copy = createCopy();

        assertEquals(copy, copy);
    }

    @Test
    void equals_nullOrDifferentType_returnsFalse() {
        BookCopy copy = createCopy();

        assertAll(
                () -> assertFalse(copy.equals(null)),
                () -> assertFalse(copy.equals("not a book copy")));
    }

    @Test
    void equals_equalCopiesHaveEqualHashCodes() {
        BookCopy firstCopy = createCopy();
        BookCopy secondCopy = new BookCopy("COPY-1", "978-2", CopyStatus.LOST, "B2-02");

        assertEquals(firstCopy.hashCode(), secondCopy.hashCode());
    }

    @Test
    void toString_containsCopyDetails() {
        BookCopy copy = createCopy();
        String representation = copy.toString();

        assertAll(
                () -> assertTrue(representation.contains("COPY-1")),
                () -> assertTrue(representation.contains("978-1")),
                () -> assertTrue(representation.contains("AVAILABLE")),
                () -> assertTrue(representation.contains("A1-01")));
    }

    private static BookCopy createCopy() {
        return new BookCopy("COPY-1", "978-1", "A1-01");
    }
}
