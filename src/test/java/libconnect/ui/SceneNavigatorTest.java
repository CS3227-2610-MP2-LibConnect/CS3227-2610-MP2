package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import libconnect.ui.pages.BookInfoPage;
import libconnect.ui.pages.BorrowPage;
import libconnect.ui.pages.DashboardPage;
import libconnect.ui.pages.LoginPage;
import libconnect.ui.pages.ProfilePage;
import libconnect.ui.pages.RegistrationPage;

/** Tests application-level UI configuration shared by navigated pages. */
class SceneNavigatorTest {
    @Test
    void sharedStylesheet_isLoadedIntoApplicationScene() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                assertTrue(context.stage().getScene().getStylesheets().stream()
                        .anyMatch(stylesheet -> stylesheet.endsWith("/libconnect.css")));
            } finally {
                context.close();
            }
        });
    }

    @Test
    void authenticatedRoutes_displayExpectedPagesOrConfiguredFallbacks() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.navigator().showRegistrationPage();
                assertInstanceOf(RegistrationPage.class, context.stage().getScene().getRoot());

                context.navigator().showDashboardPage();
                assertInstanceOf(DashboardPage.class, context.stage().getScene().getRoot());

                context.navigator().showBorrowPage();
                assertInstanceOf(BorrowPage.class, context.stage().getScene().getRoot());

                context.navigator().showProfilePage();
                assertInstanceOf(ProfilePage.class, context.stage().getScene().getRoot());

                context.navigator().showBookInfoPage("UNKNOWN");
                assertInstanceOf(BookInfoPage.class, context.stage().getScene().getRoot());

                context.navigator().showNotificationsPage();
                assertInstanceOf(DashboardPage.class, context.stage().getScene().getRoot());
                context.navigator().showReservationPage();
                assertInstanceOf(DashboardPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void unauthenticatedRoutes_redirectToLoginPage() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.sessionManager().logout();
                context.navigator().showDashboardPage();
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
                context.navigator().showBorrowPage();
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
                context.navigator().showProfilePage();
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
                context.navigator().showBookInfoPage("UNKNOWN");
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
                context.navigator().showNotificationsPage();
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
                context.navigator().showReservationPage();
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }
        });
    }
}
