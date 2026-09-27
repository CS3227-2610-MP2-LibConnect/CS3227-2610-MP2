package libconnect.ui.components;

import java.util.Objects;

import javafx.scene.control.Alert;

import libconnect.models.Notification;

/** Displays the full content of a notification in a modal dialog. */
public final class NotificationDialog {
    private NotificationDialog() {
    }

    /**
     * Shows the full message for the supplied notification.
     *
     * @param notification the notification whose message should be displayed.
     */
    public static void show(Notification notification) {
        Objects.requireNonNull(notification, "notification");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notification");
        alert.setHeaderText(notification.getType().toString());
        alert.setContentText(notification.getMessage());
        alert.showAndWait();
    }
}
