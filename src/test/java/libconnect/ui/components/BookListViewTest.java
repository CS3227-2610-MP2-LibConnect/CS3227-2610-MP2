package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import org.junit.jupiter.api.Test;

import libconnect.models.Book;
import libconnect.ui.UiTestFixtures;
import libconnect.ui.UiTestSupport;

/** Tests the book list scroll pane and its book-selection behavior. */
class BookListViewTest {
    @Test
    void emptyAndPopulatedResults_renderExpectedCards() {
        UiTestSupport.runOnFxThread(() -> {
            BookListView view = new BookListView();
            view.showBooks(List.of());
            VBox emptyContent = (VBox) view.getContent();
            assertEquals("No books match your search.",
                    ((Label) emptyContent.getChildren().get(0)).getText());

            Book firstBook = UiTestFixtures.book("978-1", "First");
            Book secondBook = UiTestFixtures.book("978-2", "Second");
            view.showBooks(List.of(firstBook, secondBook));
            assertEquals(2, emptyContent.getChildren().size());
            assertTrue(UiTestSupport.labelTexts(view).contains("First"));
            assertTrue(UiTestSupport.labelTexts(view).stream()
                    .anyMatch(text -> text.contains("ISBN: 978-1")));
        });
    }

    @Test
    void primaryClickInvokesHandler_secondaryClickDoesNot() {
        UiTestSupport.runOnFxThread(() -> {
            AtomicInteger selections = new AtomicInteger();
            Book book = UiTestFixtures.book("978-1", "Title");
            BookListView view = new BookListView(selectedBook -> {
                assertSame(book, selectedBook);
                selections.incrementAndGet();
            });
            view.showBooks(List.of(book));
            VBox card = (VBox) ((VBox) view.getContent()).getChildren().get(0);

            card.fireEvent(mouseEvent(MouseButton.PRIMARY));
            card.fireEvent(mouseEvent(MouseButton.SECONDARY));
            assertEquals(1, selections.get());
        });
    }

    @Test
    void nullArguments_areRejected_andScrollPaneHasExpectedConfiguration() {
        UiTestSupport.runOnFxThread(() -> {
            assertThrows(NullPointerException.class, () -> new BookListView(null));
            BookListView view = new BookListView();
            assertThrows(NullPointerException.class, () -> view.showBooks(null));
            assertTrue(view.isFitToWidth());
            assertEquals(320, view.getPrefHeight());
        });
    }

    private static MouseEvent mouseEvent(MouseButton button) {
        return new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, button, 1,
                false, false, false, false, button == MouseButton.PRIMARY,
                button == MouseButton.MIDDLE, button == MouseButton.SECONDARY,
                false, false, true, null);
    }
}
