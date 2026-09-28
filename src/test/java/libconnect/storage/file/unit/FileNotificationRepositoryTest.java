package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.storage.StorageManager;
import libconnect.storage.file.FileNotificationRepository;

/** Tests recipient and idempotency queries in the file-backed repository. */
class FileNotificationRepositoryTest extends AbstractFileRepositoryTest<Notification,
        FileNotificationRepository> {

    @Override
    protected FileNotificationRepository createRepository(Path dataFile) {
        return new FileNotificationRepository(new StorageManager(temporaryDirectory), dataFile);
    }

    @Override
    protected void save(FileNotificationRepository repository, Notification notification) {
        repository.save(notification);
    }

    @Override
    protected List<Notification> findAll(FileNotificationRepository repository) {
        return repository.findAll();
    }

    @Override
    protected String getIdentifier(Notification notification) {
        return notification.getId();
    }

    @Override
    protected Notification createEntity(String identifier, String variant) {
        return new Notification(identifier, "USER-" + variant, NotificationType.OVERDUE_ALERT,
                "REF-" + variant, "Overdue loan", LocalDateTime.of(2026, 9, 1, 10, 0), false);
    }

    @Test
    void queries_isolateRecipientsAndMatchTypeAndReference() {
        FileNotificationRepository repository = createRepository(
                temporaryDirectory.resolve("notifications.json"));
        Notification notification = createEntity("NOTIFICATION-1", "1");
        repository.save(notification);

        assertTrue(repository.findByRecipientUserId("USER-1").stream()
                .anyMatch(saved -> saved.getId().equals(notification.getId())));
        assertTrue(repository.findByRecipientUserId("USER-2").isEmpty());
        assertTrue(repository.existsByRecipientAndReference("USER-1",
                NotificationType.OVERDUE_ALERT, "REF-1"));
        assertFalse(repository.existsByRecipientAndReference("USER-1",
                NotificationType.RESERVATION_REMINDER, "REF-1"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void findByRecipientUserId_blankId_throwsIllegalArgumentException(String recipientUserId) {
        FileNotificationRepository repository = createRepository(
                temporaryDirectory.resolve("notifications.json"));

        assertThrows(IllegalArgumentException.class,
                () -> repository.findByRecipientUserId(recipientUserId));
    }

    @Test
    void existsByRecipientAndReference_nullType_throwsNullPointerException() {
        FileNotificationRepository repository = createRepository(
                temporaryDirectory.resolve("notifications.json"));

        assertThrows(NullPointerException.class,
                () -> repository.existsByRecipientAndReference("USER-1", null, "REF-1"));
    }
}
