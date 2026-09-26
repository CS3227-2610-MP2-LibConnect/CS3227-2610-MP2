package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.ui.pages.DashboardPage;

/** Tests dashboard loading, search, recommendations, and failure states. */
class DashboardPageTest {
    @Test
    void initialLoadSearchAndFailure_renderExpectedStates() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Algorithms"));
                DashboardPage page = new DashboardPage(context.bookService(),
                        context.sessionManager(), context.navigator(), "Borrowed.");
                assertTrue(UiTestSupport.labelTexts(page).contains("Welcome, Alex Member!"));
                assertTrue(UiTestSupport.labelTexts(page).contains("Algorithms"));
                UiPageTestSupport.assertFeedback(page, "Borrowed.");

                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Algorithms");
                UiTestSupport.findButton(page, "Search").fire();
                assertEquals("Algorithms", context.bookService().lastCriteria.getTitle());

                context.bookService().searchFailure = UiPageTestSupport.repositoryFailure();
                UiTestSupport.findButton(page, "Search").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to access book data. Please try again.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void initialLoadFailure_showsStorageErrorAndEmptyList() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().allBooksFailure = UiPageTestSupport.repositoryFailure();
                DashboardPage page = new DashboardPage(context.bookService(),
                        context.sessionManager(), context.navigator());
                UiPageTestSupport.assertFeedback(page, "Unable to access book data. Please try again.");
                assertTrue(UiTestSupport.labelTexts(page).contains("no books in database"));
                assertTrue(UiTestSupport.labelTexts(page).contains("No books match your search."));
            } finally {
                context.close();
            }
        });
    }
}
