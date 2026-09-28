package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.ui.UiTestSupport;

/** Tests notification summaries, previews, styles, and selection callbacks. */
class NotificationCardTest {
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 21, 13, 45);

    @Test
    void displaysTypePreviewAndTimestamp() {
        UiTestSupport.runOnFxThread(() -> {
            Notification notification = createNotification("N-1", "Short message", false);
            NotificationCard card = new NotificationCard(notification, ignored -> {
            });

            List<String> labels = UiTestSupport.labelTexts((javafx.scene.Parent) card.getGraphic());
            assertEquals(List.of("Overdue alert", "Short message",
                    CREATED_AT.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))), labels);
            assertTrue(card.getStyleClass().contains("notification-unread"));
        });
    }

    @Test
    void truncatesMessagesLongerThanPreviewLimit() {
        UiTestSupport.runOnFxThread(() -> {
            String message = "a".repeat(111);
            NotificationCard card = new NotificationCard(createNotification("N-1", message, false),
                    ignored -> {
                    });

            String preview = UiTestSupport.labelTexts((javafx.scene.Parent) card.getGraphic()).get(1);
            assertEquals(message.substring(0, 107) + "...", preview);
            assertEquals(110, preview.length());
        });
    }

    @Test
    void readNotificationUsesReadStyle() {
        UiTestSupport.runOnFxThread(() -> {
            NotificationCard card = new NotificationCard(createNotification("N-1", "Message", true),
                    ignored -> {
                    });

            assertTrue(card.getStyleClass().contains("notification-read"));
        });
    }

    @Test
    void selectingCardPassesOriginalNotificationToCallback() {
        UiTestSupport.runOnFxThread(() -> {
            Notification notification = createNotification("N-1", "Message", false);
            AtomicReference<Notification> selected = new AtomicReference<>();
            NotificationCard card = new NotificationCard(notification, selected::set);

            card.fire();

            assertEquals(notification, selected.get());
        });
    }

    @Test
    void requiredArgumentsAreValidated() {
        UiTestSupport.runOnFxThread(() -> {
            Notification notification = createNotification("N-1", "Message", false);
            assertThrows(NullPointerException.class,
                    () -> new NotificationCard(null, ignored -> {
                    }));
            assertThrows(NullPointerException.class,
                    () -> new NotificationCard(notification, null));
        });
    }

    private static Notification createNotification(String id, String message, boolean isRead) {
        return new Notification(id, "USER-1", NotificationType.OVERDUE_ALERT, "LOAN-1",
                message, CREATED_AT, isRead);
    }
}
