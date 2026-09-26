package libconnect.ui;

import javafx.application.Application;
import javafx.stage.Stage;

import libconnect.services.AuthenticationService;
import libconnect.services.BookCopyService;
import libconnect.services.BookService;
import libconnect.services.BorrowService;

/**
 * Starts the LibConnect desktop application.
 */
public final class LibConnectApplication extends Application {
    /**
     * Creates and displays the initial JavaFX application window.
     *
     * @param stage the primary application window.
     */
    @Override
    public void start(Stage stage) {
        AuthenticationService authenticationService = new AuthenticationService();
        BookCopyService bookCopyService = new BookCopyService();
        BookService bookService = new BookService();
        BorrowService borrowService = new BorrowService();
        SessionManager sessionManager = new SessionManager();
        SceneNavigator sceneNavigator = new SceneNavigator(stage, authenticationService,
                sessionManager, bookService, bookCopyService, borrowService);

        sceneNavigator.showLoginPage();
    }
}
