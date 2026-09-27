package libconnect.ui.components;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import libconnect.models.Notification;

/** Displays a clickable summary of one notification. */
public final class NotificationCard extends Button {
    private static final int PREVIEW_LENGTH = 110;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    /**
     * Creates a notification card that invokes the supplied action when selected.
     *
     * @param notification the notification represented by this card.
     * @param readAction the action used to open the full notification.
     */
    public NotificationCard(Notification notification, Consumer<Notification> readAction) {
        Objects.requireNonNull(notification, "notification");
        Objects.requireNonNull(readAction, "readAction");

        Label title = new Label(formatType(notification));
        title.getStyleClass().add("notification-title");
        Label preview = new Label(createPreview(notification.getMessage()));
        preview.getStyleClass().add("notification-preview");
        Label timestamp = new Label(notification.getCreatedAt().format(DATE_FORMATTER));
        timestamp.getStyleClass().add("notification-date");

        VBox content = new VBox(4, title, preview, timestamp);
        content.setAlignment(Pos.CENTER_LEFT);
        setGraphic(content);
        setMaxWidth(Double.MAX_VALUE);
        setAlignment(Pos.CENTER_LEFT);
        setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        getStyleClass().add("notification-card");
        getStyleClass().add(notification.isRead() ? "notification-read" : "notification-unread");
        setOnAction(event -> readAction.accept(notification));
    }

    /** Returns a readable notification type label. */
    private static String formatType(Notification notification) {
        String type = notification.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(type.charAt(0)) + type.substring(1);
    }

    /** Returns a short preview without cutting a message in the middle of a word where possible. */
    private static String createPreview(String message) {
        if (message.length() <= PREVIEW_LENGTH) {
            return message;
        }
        return message.substring(0, PREVIEW_LENGTH - 3).trim() + "...";
    }
}
