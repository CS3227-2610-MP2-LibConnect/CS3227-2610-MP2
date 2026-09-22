package libconnect.ui;

import javafx.application.Application;
import javafx.stage.Stage;

import libconnect.services.AuthenticationService;
import libconnect.services.BookService;

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
        BookService bookService = new BookService();
        SessionManager sessionManager = new SessionManager();
        SceneNavigator sceneNavigator = new SceneNavigator(stage, authenticationService,
                sessionManager, bookService);

        sceneNavigator.showLoginPage();
    }
}
