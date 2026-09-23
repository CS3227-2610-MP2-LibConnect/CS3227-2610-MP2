package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.models.CopyStatus;
import libconnect.ui.pages.BookInfoPage;

/** Tests book information and copy-availability rendering. */
class BookInfoPageTest {
    @Test
    void rendersVisibleCopiesAndEmptyStates() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Title"));
                context.bookCopyService().copies.put("A", UiTestFixtures.copy("A", "978-1",
                        CopyStatus.AVAILABLE));
                context.bookCopyService().copies.put("B", UiTestFixtures.copy("B", "978-1",
                        CopyStatus.BORROWED));
                context.bookCopyService().copies.put("C", UiTestFixtures.copy("C", "978-1",
                        CopyStatus.LOST));
                BookInfoPage page = new BookInfoPage(context.bookService(), context.bookCopyService(),
                        context.sessionManager(), "978-1", context.navigator());
                List<String> labels = UiTestSupport.labelTexts(page);
                assertTrue(labels.contains("A — Shelf location: A-1"));
                assertTrue(labels.contains("B — Borrowed"));
                assertTrue(labels.stream().noneMatch(text -> text.startsWith("C —")));

                context.bookCopyService().copies.clear();
                page = new BookInfoPage(context.bookService(), context.bookCopyService(),
                        context.sessionManager(), "978-1", context.navigator());
                assertTrue(UiTestSupport.labelTexts(page)
                        .contains("No available or borrowed copies."));
            } finally {
                context.close();
            }
        });
    }
}
