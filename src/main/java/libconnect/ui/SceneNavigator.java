package libconnect.ui;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import libconnect.services.AuthenticationService;
import libconnect.ui.pages.LoginPage;
import libconnect.ui.pages.PlaceholderPage;
import libconnect.ui.pages.RegistrationPage;

/** Coordinates page transitions within the LibConnect application window. */
public final class SceneNavigator {
    private static final double WINDOW_WIDTH = 900;
    private static final double WINDOW_HEIGHT = 600;
    private static final String APPLICATION_TITLE = "LibConnect";

    private final Stage stage;
    private final AuthenticationService authenticationService;
    private final SessionManager sessionManager;
    private final Scene scene;

    /**
     * Creates a navigator for the supplied application window and dependencies.
     *
     * @param stage the application window used for navigation.
     * @param authenticationService the service used by the login page.
     * @param sessionManager the session shared by authenticated pages.
     */
    public SceneNavigator(Stage stage, AuthenticationService authenticationService,
                          SessionManager sessionManager) {
        this.stage = stage;
        this.authenticationService = authenticationService;
        this.sessionManager = sessionManager;
        this.scene = new Scene(new javafx.scene.layout.StackPane(), WINDOW_WIDTH,
                WINDOW_HEIGHT);
        this.stage.setTitle(APPLICATION_TITLE);
        this.stage.setScene(scene);
        this.stage.setMinWidth(WINDOW_WIDTH);
        this.stage.setMinHeight(WINDOW_HEIGHT);
    }

    /**
     * Displays the login page and removes any previous authenticated page.
     */
    public void showLoginPage() {
        showLoginPage(null);
    }

    /**
     * Displays the login page with an optional success message.
     *
     * @param successMessage the message displayed after a successful operation, or null.
     */
    public void showLoginPage(String successMessage) {
        showPage(new LoginPage(authenticationService, sessionManager, this, successMessage));
    }

    /** Displays the account registration page. */
    public void showRegistrationPage() {
        showPage(new RegistrationPage(authenticationService, this));
    }

    /**
     * Displays the authenticated placeholder page.
     *
     * If no user is currently logged in, the navigator returns to the login page.
     */
    public void showPlaceholderPage() {
        if (!sessionManager.isLoggedIn()) {
            showLoginPage();
            return;
        }

        showPage(new PlaceholderPage(sessionManager, this));
    }

    private void showPage(Parent page) {
        scene.setRoot(page);
        stage.show();
    }
}
