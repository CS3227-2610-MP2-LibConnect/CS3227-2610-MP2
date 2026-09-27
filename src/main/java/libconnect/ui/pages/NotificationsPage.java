package libconnect.ui.pages;

import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import libconnect.models.Notification;
import libconnect.models.User;
import libconnect.services.NotificationService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.Navbar;
import libconnect.ui.components.NotificationCard;
import libconnect.ui.components.NotificationDialog;
import libconnect.ui.components.PageHeader;

/** Displays all notifications for the authenticated member. */
public final class NotificationsPage extends BorderPane {
    private final NotificationService notificationService;
    private final SessionManager sessionManager;
    private final VBox notificationList;
    private final FeedbackMessage feedbackMessage;

    /**
     * Creates a notifications page backed by the supplied service and session.
     *
     * @param notificationService the service used to load and mark notifications.
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used for page transitions.
     */
    public NotificationsPage(NotificationService notificationService, SessionManager sessionManager,
                             SceneNavigator sceneNavigator) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        Objects.requireNonNull(sceneNavigator, "sceneNavigator");
        notificationList = new VBox(10);
        feedbackMessage = new FeedbackMessage();

        Navbar navbar = new Navbar(sceneNavigator, sessionManager.getCurrentUser(), Navbar.ActivePage.NONE, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
        VBox content = new VBox(16,
                new PageHeader("Notifications", "View messages from LibConnect"),
                feedbackMessage,
                notificationList);
        content.setPadding(new Insets(20));

        ScrollPane pageScrollPane = new ScrollPane(content);
        pageScrollPane.setFitToWidth(true);
        pageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setTop(navbar);
        setCenter(pageScrollPane);
        loadNotifications();
    }

    /** Loads all notifications for the current authenticated user. */
    private void loadNotifications() {
        notificationList.getChildren().clear();
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) {
            showError("Only an authenticated member can view notifications.");
            return;
        }

        try {
            List<Notification> notifications = notificationService.getNotifications(currentUser.getId());
            if (notifications.isEmpty()) {
                Label emptyLabel = new Label("You have no notifications.");
                emptyLabel.getStyleClass().add("notification-empty");
                notificationList.getChildren().add(emptyLabel);
                return;
            }

            notifications.forEach(notification -> notificationList.getChildren().add(
                    new NotificationCard(notification, this::openNotification)));
        } catch (RepositoryException | IllegalArgumentException exception) {
            showError("Unable to load notifications. Please try again.");
        }
    }

    /** Opens a notification's full message and persists its read state. */
    private void openNotification(Notification notification) {
        try {
            notificationService.markAsRead(notification.getId());
            NotificationDialog.show(notification);
            loadNotifications();
        } catch (RepositoryException | IllegalArgumentException exception) {
            showError("Unable to open notification. Please try again.");
        }
    }

    /** Displays a user-facing notification error. */
    private void showError(String message) {
        feedbackMessage.showError(message);
    }
}
