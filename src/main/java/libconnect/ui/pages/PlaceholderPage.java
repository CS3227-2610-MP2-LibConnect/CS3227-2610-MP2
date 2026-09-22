package libconnect.ui.pages;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import libconnect.models.User;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.PageContainer;
import libconnect.ui.components.PageHeader;

/** Provides the initial authenticated placeholder page. */
public final class PlaceholderPage extends BorderPane {
    /**
     * Creates a placeholder page for the currently authenticated user.
     *
     * @param sessionManager the session containing the authenticated user.
     * @param sceneNavigator the navigator used to return to the login page.
     */
    public PlaceholderPage(SessionManager sessionManager, SceneNavigator sceneNavigator) {
        User currentUser = sessionManager.getCurrentUser();
        String displayName = currentUser == null ? "user" : currentUser.getName();

        Button logoutButton = new Button("Logout");
        logoutButton.setOnAction(event -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });

        PageContainer pageContainer = new PageContainer(
                new PageHeader("LibConnect", "You are logged in"),
                new Label("Welcome, " + displayName + "!"),
                new Label("The authenticated application area will be added here."),
                logoutButton);
        pageContainer.setPadding(new Insets(32));
        setCenter(pageContainer);
        setPadding(new Insets(24));
    }
}
