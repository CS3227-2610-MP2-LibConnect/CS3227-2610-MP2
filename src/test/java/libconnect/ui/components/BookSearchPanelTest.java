package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests simple and advanced search form behavior. */
class BookSearchPanelTest {
    @Test
    void simpleAndAdvancedSearch_buildExpectedCriteria() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicReference<libconnect.services.BookSearchCriteria> criteria = new AtomicReference<>();
            AtomicReference<String> error = new AtomicReference<>("unexpected");
            BookSearchPanel panel = new BookSearchPanel(criteria::set, error::set);
            TextField simpleTitle = UiTestSupport.findTextFields(panel).get(0);
            simpleTitle.setText("  Algorithms ");
            UiTestSupport.findButton(panel, "Search").fire();
            assertEquals("Algorithms", criteria.get().getTitle());
            assertNull(error.get());

            ComboBox<?> selector = UiTestSupport.findNodes(panel, ComboBox.class).get(0);
            selector.getSelectionModel().select(1);
            List<TextField> fields = UiTestSupport.findTextFields(panel);
            fields.get(1).setText("978-1");
            fields.get(2).setText("Title");
            fields.get(3).setText("Author");
            fields.get(4).setText("Publisher");
            fields.get(5).setText("Category");
            fields.get(6).setText("2000");
            fields.get(7).setText("2020");
            UiTestSupport.findButton(panel, "Search").fire();
            assertEquals("978-1", criteria.get().getIsbn());
            assertEquals(2000, criteria.get().getPublicationYearStart());
            assertEquals(2020, criteria.get().getPublicationYearEnd());
        });
    }

    @Test
    void invalidYearAndReset_reportErrorAndClearFields() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger searches = new AtomicInteger();
            AtomicReference<String> error = new AtomicReference<>();
            BookSearchPanel panel = new BookSearchPanel(criteria -> searches.incrementAndGet(),
                    error::set);
            ComboBox<?> selector = UiTestSupport.findNodes(panel, ComboBox.class).get(0);
            selector.getSelectionModel().select(1);
            List<TextField> fields = UiTestSupport.findTextFields(panel);
            fields.get(6).setText("0");
            UiTestSupport.findButton(panel, "Search").fire();
            assertEquals("Start year must be positive.", error.get());
            assertEquals(0, searches.get());

            fields.get(0).setText("978-1");
            UiTestSupport.findButton(panel, "Reset").fire();
            assertEquals(1, searches.get());
            assertTrue(UiTestSupport.findTextFields(panel).stream()
                    .allMatch(field -> field.getText().isEmpty()));
        });
    }
}
