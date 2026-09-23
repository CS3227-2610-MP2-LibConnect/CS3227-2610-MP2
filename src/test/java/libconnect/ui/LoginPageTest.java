package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountType;
import libconnect.services.AuthenticationException;
import libconnect.storage.repositories.RepositoryException;
import libconnect.ui.pages.DashboardPage;
import libconnect.ui.pages.LoginPage;
import libconnect.ui.pages.RegistrationPage;

/** Tests login validation, authentication feedback, and navigation. */
class LoginPageTest {
    @Test
    void validationAndSuccessfulLogin_updateSessionAndNavigate() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                LoginPage page = new LoginPage(context.authenticationService(),
                        context.sessionManager(), context.navigator());
                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText(" ");
                fields.get(1).setText(" ");
                UiTestSupport.findButton(page, "Login").fire();
                UiPageTestSupport.assertFeedback(page, "Username and password are required.");

                fields.get(0).setText("alex@example.com");
                fields.get(1).setText("secret");
                UiTestSupport.findButton(page, "Login").fire();
                assertEquals(AccountType.MEMBER, context.authenticationService().lastAccountType);
                assertEquals("alex@example.com", context.authenticationService().lastEmail);
                assertEquals("secret", context.authenticationService().lastPassword);
                assertInstanceOf(DashboardPage.class, context.stage().getScene().getRoot());
                assertTrue(context.sessionManager().isLoggedIn());
                assertEquals("", fields.get(1).getText());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void authenticationAndRepositoryFailures_clearPasswordAndShowErrors() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                context.authenticationService().authenticateFailure =
                        new AuthenticationException("Invalid email or password.");
                LoginPage page = new LoginPage(context.authenticationService(),
                        context.sessionManager(), context.navigator());
                UiPageTestSupport.fillLoginFields(page);
                UiTestSupport.findButton(page, "Login").fire();
                UiPageTestSupport.assertFeedback(page, "Invalid email or password.");

                context.authenticationService().authenticateFailure =
                        new RepositoryException("storage failure", new IllegalStateException("failure"));
                UiPageTestSupport.fillLoginFields(page);
                UiTestSupport.findButton(page, "Login").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to access account data. Please try again.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void createAccountAndSuccessMessage_areRendered() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                LoginPage page = new LoginPage(context.authenticationService(),
                        context.sessionManager(), context.navigator(), "Account created.");
                UiPageTestSupport.assertFeedback(page, "Account created.");
                UiTestSupport.findButton(page, "Create account").fire();
                assertInstanceOf(RegistrationPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }
        });
    }
}
