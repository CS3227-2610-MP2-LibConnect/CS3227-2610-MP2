package libconnect.services.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.services.BookSearchCriteria;

/** Tests normalization and validation of book search criteria. */
class BookSearchCriteriaTest {
    @Test
    void constructor_trimsTextAndPreservesYearBounds() {
        BookSearchCriteria criteria = new BookSearchCriteria(" 978-1 ", " title ", " author ",
                " publisher ", " category ", 2000, 2020);

        assertEquals("978-1", criteria.getIsbn());
        assertEquals("title", criteria.getTitle());
        assertEquals("author", criteria.getAuthor());
        assertEquals("publisher", criteria.getPublisher());
        assertEquals("category", criteria.getCategory());
        assertEquals(2000, criteria.getPublicationYearStart());
        assertEquals(2020, criteria.getPublicationYearEnd());
        assertFalse(criteria.isEmpty());
    }

    @Test
    void constructor_nullAndBlankCriteria_areEmpty() {
        BookSearchCriteria criteria = new BookSearchCriteria(null, "   ", null, "", null, null, null);

        assertTrue(criteria.isEmpty());
        assertNull(criteria.getIsbn());
        assertNull(criteria.getTitle());
    }

    @Test
    void constructor_invalidYears_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new BookSearchCriteria(null, null, null, null, null, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> new BookSearchCriteria(null, null, null, null, null, null, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new BookSearchCriteria(null, null, null, null, null, 2021, 2020));
    }

    @Test
    void constructor_openEndedRanges_areAccepted() {
        assertEquals(2020, new BookSearchCriteria(null, null, null, null, null, 2020, null)
                .getPublicationYearStart());
        assertEquals(2020, new BookSearchCriteria(null, null, null, null, null, null, 2020)
                .getPublicationYearEnd());
    }
}
