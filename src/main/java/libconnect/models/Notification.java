package libconnect.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/** Represents an in-app notification delivered to a user. */
public final class Notification implements libconnect.storage.repositories.Identifiable {
    private final String notificationId;
    private final String recipientUserId;
    private final NotificationType type;
    private final String referenceId;
    private final String message;
    private final LocalDateTime createdAt;
    private final boolean read;

    /** Creates an unread notification with an optional idempotency reference. */
    public Notification(String notificationId, String recipientUserId, NotificationType type,
                        String referenceId, String message, LocalDateTime createdAt,
                        boolean read) {
        this.notificationId = requireText(notificationId, "notificationId");
        this.recipientUserId = requireText(recipientUserId, "recipientUserId");
        this.type = Objects.requireNonNull(type, "type");
        this.referenceId = requireText(referenceId, "referenceId");
        this.message = requireText(message, "message");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.read = read;
    }

    /** Returns the stable notification identifier. */
    @Override
    @JsonProperty("notificationId")
    public String getId() {
        return notificationId;
    }

    /** Returns the recipient user identifier. */
    public String getRecipientUserId() {
        return recipientUserId;
    }

    /** Returns the notification category. */
    public NotificationType getType() {
        return type;
    }

    /** Returns the domain identifier that makes notification delivery idempotent. */
    public String getReferenceId() {
        return referenceId;
    }

    /** Returns the notification message. */
    public String getMessage() {
        return message;
    }

    /** Returns the creation timestamp. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Returns whether the recipient has read this notification. */
    public boolean isRead() {
        return read;
    }

    /** Returns a copy marked as read. */
    public Notification markRead() {
        return new Notification(notificationId, recipientUserId, type, referenceId,
                message, createdAt, true);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
