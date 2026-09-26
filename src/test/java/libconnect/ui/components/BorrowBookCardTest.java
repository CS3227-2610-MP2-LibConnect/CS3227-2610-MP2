package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import libconnect.models.Book;
import libconnect.models.BookCopy;
import libconnect.models.CopyStatus;
import libconnect.ui.UiTestFixtures;
import libconnect.ui.UiTestSupport;

/** Tests borrow-book card rendering and removal behavior. */
class BorrowBookCardTest {
    @Test
    void rendersDetailsAndRemoveAction() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger removals = new AtomicInteger();
            Book book = UiTestFixtures.book("978-1", "Title");
            BookCopy copy = UiTestFixtures.copy("COPY-1", "978-1", CopyStatus.AVAILABLE);
            BorrowBookCard card = new BorrowBookCard(book, copy, removals::incrementAndGet);
            assertEquals("COPY-1", card.getCopyId());
            assertTrue(UiTestSupport.labelTexts(card).contains("Title:"));
            assertTrue(UiTestSupport.labelTexts(card).contains("COPY-1"));
            UiTestSupport.findButton(card, "Remove").fire();
            assertEquals(1, removals.get());
        });
    }
}
