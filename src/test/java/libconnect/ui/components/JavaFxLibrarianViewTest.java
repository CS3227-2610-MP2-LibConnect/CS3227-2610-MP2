package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.control.Label;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests JavaFX status updates made by the librarian view adapter. */
class JavaFxLibrarianViewTest {
    @Test
    void messageUpdatesBoundLabelWithSuccessStyle() {
        UiTestSupport.runOnFxThread(() -> {
            JavaFxLibrarianView view = new JavaFxLibrarianView();
            Label label = new Label();
            view.bind(label);

            view.showMessage("Saved");

            assertEquals("Saved", label.getText());
            assertEquals("-fx-text-fill: #176b2c;", label.getStyle());
        });
    }

    @Test
    void errorUpdatesBoundLabelWithErrorStyle() {
        UiTestSupport.runOnFxThread(() -> {
            JavaFxLibrarianView view = new JavaFxLibrarianView();
            Label label = new Label();
            view.bind(label);

            view.showError("Unable to save");

            assertEquals("Unable to save", label.getText());
            assertEquals("-fx-text-fill: #b00020;", label.getStyle());
        });
    }

    @Test
    void messageBeforeBindingIsIgnored() {
        UiTestSupport.runOnFxThread(() -> {
            JavaFxLibrarianView view = new JavaFxLibrarianView();

            view.showMessage("Not displayed");
        });
    }

    @Test
    void backgroundThreadMessageIsAppliedOnFxThread() throws InterruptedException {
        JavaFxLibrarianView view = new JavaFxLibrarianView();
        AtomicReference<Label> labelReference = new AtomicReference<>();
        UiTestSupport.runOnFxThread(() -> {
            Label label = new Label();
            labelReference.set(label);
            view.bind(label);
        });

        Thread worker = new Thread(() -> view.showMessage("Background update"));
        worker.start();
        worker.join();

        UiTestSupport.runOnFxThread(() -> assertEquals("Background update",
                labelReference.get().getText()));
    }

    @Test
    void nullArgumentsAreRejected() {
        UiTestSupport.runOnFxThread(() -> {
            JavaFxLibrarianView view = new JavaFxLibrarianView();
            assertThrows(NullPointerException.class, () -> view.bind(null));
            assertThrows(NullPointerException.class, () -> view.showMessage(null));
            assertThrows(NullPointerException.class, () -> view.showError(null));
        });
    }
}
