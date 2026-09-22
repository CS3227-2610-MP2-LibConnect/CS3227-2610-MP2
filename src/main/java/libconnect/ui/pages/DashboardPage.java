package libconnect.ui.pages;

import java.util.List;
import java.util.Objects;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import libconnect.models.Book;
import libconnect.models.User;
import libconnect.services.BookSearchCriteria;
import libconnect.services.BookService;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.BookListView;
import libconnect.ui.components.BookSearchPanel;
import libconnect.ui.components.DashboardNavbar;
import libconnect.ui.components.FeedbackMessage;
import libconnect.ui.components.PageHeader;
import libconnect.ui.components.RecommendationBanner;

/** Provides the authenticated LibConnect dashboard. */
public final class DashboardPage extends BorderPane {
    private static final String STORAGE_ERROR_MESSAGE =
            "Unable to access book data. Please try again.";

    private final BookService bookService;
    private final RecommendationBanner recommendationBanner;
    private final BookListView bookListView;
    private final FeedbackMessage feedbackMessage;

    /**
     * Creates a dashboard backed by the supplied session, navigation, and catalogue services.
     *
     * @param bookService the service used to load and search books.
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used for logout.
     */
    public DashboardPage(BookService bookService, SessionManager sessionManager,
                         SceneNavigator sceneNavigator) {
        this(bookService, sessionManager, sceneNavigator, null);
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
        this.bookService = Objects.requireNonNull(bookService, "bookService");
        recommendationBanner = new RecommendationBanner();
        bookListView = new BookListView(book -> sceneNavigator.showBookInfoPage(book.getIsbn()));
        feedbackMessage = new FeedbackMessage();

        DashboardNavbar navbar = new DashboardNavbar(sceneNavigator::showDashboardPage,
                sceneNavigator::showBorrowPage, sceneNavigator::showMyLoansPage, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        }, DashboardNavbar.ActivePage.DASHBOARD);
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
