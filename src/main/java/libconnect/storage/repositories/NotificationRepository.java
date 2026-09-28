package libconnect.storage.repositories;

import java.util.List;

import libconnect.models.Notification;
import libconnect.models.NotificationType;

/** Defines notification persistence and idempotency queries. */
public interface NotificationRepository extends Repository<Notification> {
    /** Returns notifications for a recipient. */
    List<Notification> findByRecipientUserId(String recipientUserId);

    /** Checks whether a notification for a reference was already delivered. */
    boolean existsByRecipientAndReference(String recipientUserId, NotificationType type,
                                          String referenceId);
}
