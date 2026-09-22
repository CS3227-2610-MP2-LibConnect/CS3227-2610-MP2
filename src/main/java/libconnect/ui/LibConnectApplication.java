package libconnect.ui;

import javafx.application.Application;
import javafx.stage.Stage;

import libconnect.services.AuthenticationService;

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
        SessionManager sessionManager = new SessionManager();
        SceneNavigator sceneNavigator = new SceneNavigator(stage, authenticationService,
                sessionManager);

        sceneNavigator.showLoginPage();
    }
}
