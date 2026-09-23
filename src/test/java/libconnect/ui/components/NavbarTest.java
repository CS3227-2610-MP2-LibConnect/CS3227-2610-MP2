package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import javafx.stage.Stage;

import libconnect.services.AuthenticationService;
import libconnect.ui.SceneNavigator;
import libconnect.ui.SessionManager;
import libconnect.ui.UiTestFixtures;
import libconnect.ui.UiTestSupport;

/** Tests dashboard navigation button state and callbacks. */
class NavbarTest {
    @Test
    void activePageDisablesOnlyMatchingNavigationButton() {
        UiTestSupport.runOnFxThread(() -> {
            Stage stage = new Stage();
            try {
                SessionManager sessionManager = new SessionManager();
                sessionManager.login(UiTestFixtures.member());
                SceneNavigator sceneNavigator = new SceneNavigator(stage,
                        new AuthenticationService(), sessionManager);
                Navbar navbar = new Navbar(sceneNavigator, sessionManager.getCurrentUser(),
                        Navbar.ActivePage.BORROW, () -> {
                        });
                UiTestSupport.findButtons(navbar).forEach(button -> {
                    if (button.getText().equals("Borrow books")) {
                        assertTrue(button.isDisable());
                    } else {
                        assertFalse(button.isDisable());
                    }
                });
            } finally {
                stage.close();
            }
        });
    }
}
