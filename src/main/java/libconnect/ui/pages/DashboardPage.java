package libconnect.ui.pages;

import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.Notification;
import libconnect.models.User;
import libconnect.services.BookSearchCriteria;
import libconnect.services.BookService;
import libconnect.services.NotificationService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.BookListView;
import libconnect.ui.components.BookSearchPanel;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.Navbar;
import libconnect.ui.components.NotificationCard;
import libconnect.ui.components.NotificationDialog;
import libconnect.ui.components.PageHeader;
import libconnect.ui.components.RecommendationBanner;

/** Provides the authenticated LibConnect dashboard. */
public final class DashboardPage extends BorderPane {
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access book data. Please try again.";

    private final BookService bookService;
    private final SessionManager sessionManager;
    private final RecommendationBanner recommendationBanner;
    private final BookListView bookListView;
    private final FeedbackMessage feedbackMessage;
    private final NotificationService notificationService;
    private final VBox unreadNotificationList;

    /**
     * Creates a dashboard backed by the supplied session, navigation, and catalogue services.
     *
     * @param bookService the service used to load and search books.
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used for logout.
     */
    public DashboardPage(BookService bookService, SessionManager sessionManager,
                         SceneNavigator sceneNavigator) {
        this(bookService, sessionManager, sceneNavigator, null, null);
    }

    /**
     * Creates a dashboard with an optional success message.
     *
     * @param bookService the service used to load and search books.
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used for dashboard actions.
     * @param successMessage the message displayed after a successful operation, or null.
     */
    public DashboardPage(BookService bookService, SessionManager sessionManager,
                         SceneNavigator sceneNavigator, String successMessage) {
        this(bookService, sessionManager, sceneNavigator, successMessage, null);
    }

    /**
     * Creates a dashboard with notification support and an optional success message.
     *
     * @param bookService the service used to load and search books.
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used for dashboard actions.
     * @param successMessage the message displayed after a successful operation, or null.
     * @param notificationService the service used to load and mark notifications.
     */
    public DashboardPage(BookService bookService, SessionManager sessionManager,
                         SceneNavigator sceneNavigator, String successMessage,
                         NotificationService notificationService) {
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        this.sessionManager = Objects.requireNonNull(sessionManager, "sessionManager");
        this.notificationService = notificationService;
        recommendationBanner = new RecommendationBanner();
        bookListView = new BookListView(book -> sceneNavigator.showBookInfoPage(book.getIsbn()));
        feedbackMessage = new FeedbackMessage();
        unreadNotificationList = new VBox(8);

        Navbar navbar = new Navbar(sceneNavigator, sessionManager.getCurrentUser(), Navbar.ActivePage.DASHBOARD, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
        BookSearchPanel searchPanel = new BookSearchPanel(this::searchBooks, this::showError);

        User currentUser = sessionManager.getCurrentUser();
        String displayName = currentUser == null ? "user" : currentUser.getName();
        Label welcomeLabel = new Label("Welcome, " + displayName + "!");

        if (successMessage != null && !successMessage.isBlank()) {
            feedbackMessage.showSuccess(successMessage);
        }
        feedbackMessage.setPadding(new Insets(8, 20, 8, 20));

        VBox content = new VBox(16,
                new PageHeader("Dashboard", "Find your next book in LibConnect"),
                welcomeLabel,
                createNotificationSection(sceneNavigator),
                recommendationBanner,
                searchPanel,
                bookListView);
        content.setPadding(new Insets(20));
        VBox.setVgrow(bookListView, Priority.ALWAYS);

        ScrollPane pageScrollPane = new ScrollPane(content);
        pageScrollPane.setFitToWidth(true);
        pageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        setTop(new VBox(navbar, feedbackMessage));
        setCenter(pageScrollPane);
        loadInitialBooks();
        loadUnreadNotifications();
    }

    /** Creates the dashboard section containing unread notifications and its page link. */
    private VBox createNotificationSection(SceneNavigator sceneNavigator) {
        BorderPane heading = new BorderPane();
        Label sectionLabel = new Label("Notifications");
        sectionLabel.getStyleClass().add("section-heading");

        Button viewAllButton = new Button("View all");
        viewAllButton.setOnAction(event -> sceneNavigator.showNotificationsPage());
        viewAllButton.getStyleClass().add("button-link");

        heading.setLeft(sectionLabel);
        heading.setRight(viewAllButton);
        heading.getStyleClass().add("notification-section-header");

        VBox section = new VBox(10, heading, unreadNotificationList);
        section.getStyleClass().add("notification-section");
        return section;
    }

    /** Loads the current member's unread notifications for the dashboard preview. */
    private void loadUnreadNotifications() {
        unreadNotificationList.getChildren().clear();
        User currentUser = getCurrentUser();
        if (notificationService == null || currentUser == null) {
            showEmptyUnreadNotifications();
            return;
        }

        try {
            List<Notification> notifications = notificationService.getNotifications(currentUser.getId())
                    .stream()
                    .filter(notification -> !notification.isRead())
                    .toList();
            if (notifications.isEmpty()) {
                showEmptyUnreadNotifications();
                return;
            }

            notifications.forEach(notification -> unreadNotificationList.getChildren().add(
                    new NotificationCard(notification, this::openNotification)));
        } catch (RepositoryException | IllegalArgumentException exception) {
            showError("Unable to load notifications. Please try again.");
        }
    }

    /** Displays the empty state for the dashboard notification preview. */
    private void showEmptyUnreadNotifications() {
        Label emptyLabel = new Label("You have no unread notifications.");
        emptyLabel.getStyleClass().add("notification-empty");
        unreadNotificationList.getChildren().add(emptyLabel);
    }

    /** Opens a notification's full message and marks it as read. */
    private void openNotification(Notification notification) {
        if (notificationService == null) {
            return;
        }

        try {
            notificationService.markAsRead(notification.getId());
            NotificationDialog.show(notification);
            loadUnreadNotifications();
        } catch (RepositoryException | IllegalArgumentException exception) {
            showError("Unable to open notification. Please try again.");
        }
    }

    /** Returns the authenticated user, or null when no user is present. */
    private User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    /** Loads all books for the initial dashboard render. */
    private void loadInitialBooks() {
        try {
            List<Book> books = bookService.getAllBooks();
            recommendationBanner.showRecommendation(books);
            bookListView.showBooks(books);
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
            recommendationBanner.showRecommendation(List.of());
            bookListView.showBooks(List.of());
        }
    }

    /**
     * Searches the catalogue and updates the scrollable result list.
     *
     * @param criteria the criteria supplied by the search panel.
     */
    private void searchBooks(BookSearchCriteria criteria) {
        try {
            bookListView.showBooks(bookService.searchBooks(criteria));
        } catch (RepositoryException exception) {
            showError(STORAGE_ERROR_MESSAGE);
            bookListView.showBooks(List.of());
        }
    }

    /**
     * Displays or clears a dashboard error message.
     *
     * @param message the error message, or null to clear the message.
     */
    private void showError(String message) {
        if (message == null || message.isBlank()) {
            feedbackMessage.clearMessage();
            return;
        }
        feedbackMessage.showError(message);
    }
}
