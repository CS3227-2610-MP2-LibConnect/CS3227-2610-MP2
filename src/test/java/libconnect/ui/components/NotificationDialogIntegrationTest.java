package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.junit.jupiter.api.Test;

import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.ui.UiTestSupport;

/** Exercises notification dialogs through the JavaFX toolkit and modal window lifecycle. */
class NotificationDialogIntegrationTest {
    @Test
    void show_displaysNotificationContentAndClosesModalDialog() {
        UiTestSupport.runOnFxThread(() -> {
            Stage owner = ownerStage();
            try {
                DialogSnapshot snapshot = showAndCapture(notification(NotificationType.OVERDUE_ALERT,
                        "The book is overdue."));

                assertEquals("OVERDUE_ALERT", snapshot.header());
                assertEquals("The book is overdue.", snapshot.content());
                assertTrue(snapshot.styleClasses().contains("information"));
            } finally {
                owner.close();
            }
        });
    }

    @Test
    void show_displaysEachNotificationTypeWithoutMixingDialogState() {
        UiTestSupport.runOnFxThread(() -> {
            Stage owner = ownerStage();
            try {
                DialogSnapshot overdue = showAndCapture(notification(NotificationType.OVERDUE_ALERT, "Overdue"));
                DialogSnapshot reminder = showAndCapture(
                        notification(NotificationType.RESERVATION_REMINDER, "Ready for collection"));

                assertEquals("OVERDUE_ALERT", overdue.header());
                assertEquals("Overdue", overdue.content());
                assertEquals("RESERVATION_REMINDER", reminder.header());
                assertEquals("Ready for collection", reminder.content());
            } finally {
                owner.close();
            }
        });
    }

    @Test
    void show_nullNotification_rejectsInputBeforeOpeningDialog() {
        UiTestSupport.runOnFxThread(() -> assertThrows(NullPointerException.class,
                () -> NotificationDialog.show(null)));
    }

    private static Stage ownerStage() {
        Stage stage = new Stage();
        stage.setScene(new Scene(new javafx.scene.layout.StackPane()));
        stage.show();
        return stage;
    }

    private static DialogSnapshot showAndCapture(Notification notification) {
        AtomicReference<DialogSnapshot> snapshot = new AtomicReference<>();
        Platform.runLater(() -> {
            DialogPane dialogPane = findDialogPane();
            snapshot.set(new DialogSnapshot(dialogPane.getHeaderText(), dialogPane.getContentText(),
                    List.copyOf(dialogPane.getStyleClass())));
            ((Button) dialogPane.lookupButton(ButtonType.OK)).fire();
        });
        NotificationDialog.show(notification);
        return snapshot.get();
    }

    private static DialogPane findDialogPane() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .map(Window::getScene)
                .filter(scene -> scene != null && scene.getRoot() instanceof DialogPane)
                .map(scene -> (DialogPane) scene.getRoot())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No notification dialog was found"));
    }

    private static Notification notification(NotificationType type, String message) {
        return new Notification("N-1", "MEM-1", type, "REF-1", message,
                LocalDateTime.of(2026, 9, 21, 10, 0), false);
    }

    private record DialogSnapshot(String header, String content, List<String> styleClasses) {
    }
}
