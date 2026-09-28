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

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import org.junit.jupiter.api.Test;

import libconnect.models.Notification;
import libconnect.models.NotificationType;
import libconnect.services.NotificationService;
import libconnect.storage.repositories.NotificationRepository;
import libconnect.ui.components.NotificationCard;
import libconnect.ui.pages.DashboardPage;

/** Tests dashboard loading, search, recommendations, and failure states. */
class DashboardPageTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"),
            ZoneOffset.UTC);

    @Test
    void initialLoadSearchAndFailure_renderExpectedStates() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().books.add(UiTestFixtures.book("978-1", "Algorithms"));
                DashboardPage page = new DashboardPage(context.bookService(),
                        context.sessionManager(), context.navigator(), "Borrowed.");
                assertTrue(UiTestSupport.labelTexts(page).contains("Welcome, Alex Member!"));
                assertTrue(UiTestSupport.labelTexts(page).contains("Algorithms"));
                UiPageTestSupport.assertFeedback(page, "Borrowed.");

                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Algorithms");
                UiTestSupport.findButton(page, "Search").fire();
                assertEquals("Algorithms", context.bookService().lastCriteria.getTitle());

                context.bookService().searchFailure = UiPageTestSupport.repositoryFailure();
                UiTestSupport.findButton(page, "Search").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to access book data. Please try again.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void initialLoadFailure_showsStorageErrorAndEmptyList() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.bookService().allBooksFailure = UiPageTestSupport.repositoryFailure();
                DashboardPage page = new DashboardPage(context.bookService(),
                        context.sessionManager(), context.navigator());
                UiPageTestSupport.assertFeedback(page, "Unable to access book data. Please try again.");
                assertTrue(UiTestSupport.labelTexts(page).contains("no books in database"));
                assertTrue(UiTestSupport.labelTexts(page).contains("No books match your search."));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void unreadNotification_isDisplayedAndOpeningMarksItRead() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            InMemoryNotificationRepository repository = new InMemoryNotificationRepository();
            Notification notification = new Notification("N-1", "MEM-1",
                    NotificationType.OVERDUE_ALERT, "LOAN-1", "Please return the overdue book.",
                    LocalDateTime.of(2026, 9, 20, 10, 0), false);
            repository.save(notification);
            try {
                DashboardPage page = new DashboardPage(context.bookService(), context.sessionManager(),
                        context.navigator(), null, new NotificationService(repository, CLOCK));
                NotificationCard card = UiTestSupport.findNodes(page, NotificationCard.class).stream()
                        .findFirst().orElseThrow();

                Platform.runLater(() -> ((Button) findNotificationDialog()
                        .lookupButton(ButtonType.OK)).fire());
                card.fire();

                assertTrue(repository.findById("N-1").orElseThrow().isRead());
                assertTrue(UiTestSupport.labelTexts(page).contains("You have no unread notifications."));
            } finally {
                context.close();
            }
        });
    }

    private static DialogPane findNotificationDialog() {
        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .map(Window::getScene)
                .filter(scene -> scene != null && scene.getRoot() instanceof DialogPane)
                .map(scene -> (DialogPane) scene.getRoot())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No notification dialog was found"));
    }

    private static final class InMemoryNotificationRepository implements NotificationRepository {
        private final Map<String, Notification> notifications = new HashMap<>();

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
