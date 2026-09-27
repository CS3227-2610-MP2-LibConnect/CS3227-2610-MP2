package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javafx.stage.Stage;

import org.junit.jupiter.api.Test;

import libconnect.models.Member;
import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.services.NotificationService;
import libconnect.storage.repositories.NotificationRepository;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.components.NotificationCard;
import libconnect.ui.pages.NotificationsPage;

/** Tests notification loading, filtering, rendering, and read-state behavior. */
class NotificationsPageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void emptyNotificationsShowEmptyMessage() {
        UiTestSupport.runOnFxThread(() -> {
            SessionManager sessionManager = loggedInSession();
            NotificationsPage page = createPage(new InMemoryNotificationRepository(), sessionManager);

            assertTrue(UiTestSupport.labelTexts(page).contains("You have no notifications."));
        });
    }

    @Test
    void notificationsAreRecipientScopedAndSortedByCreationTime() {
        UiTestSupport.runOnFxThread(() -> {
            SessionManager sessionManager = loggedInSession();
            InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
            Notification earlier = notification("N-1", "Earlier", LocalDateTime.of(2026, 9, 20, 9, 0),
                    false, "MEM-1");
            Notification later = notification("N-2", "Later", LocalDateTime.of(2026, 9, 21, 9, 0),
                    true, "MEM-1");
            repository.save(later);
            repository.save(earlier);
            repository.save(notification("N-3", "Other user", LocalDateTime.of(2026, 9, 19, 9, 0),
                    false, "OTHER-USER"));

            NotificationsPage page = createPage(repository, sessionManager);
            List<NotificationCard> cards = UiTestSupport.findNodes(page, NotificationCard.class);

            assertEquals(2, cards.size());
            assertEquals("Earlier", UiTestSupport.labelTexts(
                    (javafx.scene.Parent) cards.get(0).getGraphic()).get(1));
            assertEquals("Later", UiTestSupport.labelTexts(
                    (javafx.scene.Parent) cards.get(1).getGraphic()).get(1));
            assertTrue(cards.get(0).getStyleClass().contains("notification-unread"));
            assertTrue(cards.get(1).getStyleClass().contains("notification-read"));
        });
    }

    @Test
    void repositoryFailureShowsLoadError() {
        UiTestSupport.runOnFxThread(() -> {
            SessionManager sessionManager = loggedInSession();
            InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
            repository.findFailure = new RepositoryException("storage failure", new IllegalStateException());

            NotificationsPage page = createPage(repository, sessionManager);

            UiPageTestSupport.assertFeedback(page,
                    "Unable to load notifications. Please try again.");
        });
    }

    private static NotificationsPage createPage(InMemoryNotificationRepository repository,
                                                  SessionManager sessionManager) {
        Stage stage = new Stage();
        NotificationService service = new NotificationService(repository, CLOCK);
        SceneNavigator navigator = new SceneNavigator(stage, new libconnect.services.AuthenticationService(),
                sessionManager);
        NotificationsPage page = new NotificationsPage(service, sessionManager, navigator);
        stage.close();
        return page;
    }

    private static SessionManager loggedInSession() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.login(new Member("USER-1", "Alex Member", "alex@example.com", "hash", "MEM-1"));
        return sessionManager;
    }

    private static Notification notification(String id, String message, LocalDateTime createdAt,
                                             boolean isRead, String recipientUserId) {
        return new Notification(id, recipientUserId, NotificationType.OVERDUE_ALERT, "LOAN-1",
                message, createdAt, isRead);
    }

    private static final class InMemoryNotificationRepository implements NotificationRepository {
        private final Map<String, Notification> notifications = new HashMap<>();
        private RuntimeException findFailure;

        @Override
        public Optional<Notification> findById(String id) {
            return Optional.ofNullable(notifications.get(id));
        }

        @Override
        public boolean deleteById(String id) {
            return notifications.remove(id) != null;
        }

        @Override
        public void save(Notification notification) {
            notifications.put(notification.getId(), notification);
        }

        @Override
        public List<Notification> findAll() {
            return new ArrayList<>(notifications.values());
        }

        @Override
        public List<Notification> findByRecipientUserId(String recipientUserId) {
            if (findFailure != null) {
                throw findFailure;
            }
            return notifications.values().stream()
                    .filter(notification -> notification.getRecipientUserId().equals(recipientUserId))
                    .toList();
        }

        @Override
        public boolean existsByRecipientAndReference(String recipientUserId, NotificationType type,
                                                     String referenceId) {
            return notifications.values().stream().anyMatch(notification ->
                    notification.getRecipientUserId().equals(recipientUserId)
                            && notification.getType() == type
                            && notification.getReferenceId().equals(referenceId));
        }
    }
}
