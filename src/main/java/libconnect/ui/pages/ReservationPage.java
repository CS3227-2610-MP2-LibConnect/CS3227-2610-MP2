package libconnect.ui.pages;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.components.Navbar;

/** Displays the initial reservation page for authenticated users. */
public final class ReservationPage extends BorderPane {
    /**
     * Creates the reservation page with authenticated navigation.
     *
     * @param sessionManager the session used by the logout action.
     * @param sceneNavigator the navigator used by the navbar actions.
     */
    public ReservationPage(SessionManager sessionManager, SceneNavigator sceneNavigator) {
        Navbar navbar = new Navbar(sceneNavigator, sessionManager.getCurrentUser(), Navbar.ActivePage.RESERVATION, () -> {
            sessionManager.logout();
            sceneNavigator.showLoginPage();
        });
        setTop(navbar);
        setCenter(new Label("reservation page"));
    }
}
