package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;

import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.NotificationRepository;

/** Stores notifications in a JSON array and supports idempotency checks. */
public final class FileNotificationRepository extends AbstractFileRepository<Notification>
        implements NotificationRepository {
    /** Creates a file-backed notification repository. */
    public FileNotificationRepository(StorageManager storageManager, Path file) {
        super(storageManager, file, Notification.class);
    }

    /** Returns notifications for a recipient. */
    @Override
    public List<Notification> findByRecipientUserId(String recipientUserId) {
        requireText(recipientUserId, "recipientUserId");
        return readAll().stream()
                .filter(notification -> notification.getRecipientUserId().equals(recipientUserId))
                .toList();
    }

    /** Checks whether a notification for a reference was already delivered. */
    @Override
    public boolean existsByRecipientAndReference(String recipientUserId, NotificationType type,
                                                 String referenceId) {
        requireText(recipientUserId, "recipientUserId");
        requireText(referenceId, "referenceId");
        return readAll().stream().anyMatch(notification ->
                notification.getRecipientUserId().equals(recipientUserId)
                        && notification.getType() == type
                        && notification.getReferenceId().equals(referenceId));
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
