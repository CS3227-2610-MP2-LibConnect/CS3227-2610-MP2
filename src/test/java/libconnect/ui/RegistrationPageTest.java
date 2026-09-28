package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.models.AccountType;
import libconnect.ui.pages.LoginPage;
import libconnect.ui.pages.RegistrationPage;

/** Tests registration validation, account selection, and navigation. */
class RegistrationPageTest {
    @Test
    void accountSelectionValidationAndSuccess_workAsExpected() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                RegistrationPage page = new RegistrationPage(context.authenticationService(),
                        context.navigator());
                Button registerButton = UiTestSupport.findButton(page, "Register");
                assertTrue(registerButton.isDisable());
                ComboBox<?> selector = UiTestSupport.findNodes(page, ComboBox.class).get(0);
                selector.getSelectionModel().select(0);
                assertFalse(registerButton.isDisable());

                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Alex");
                fields.get(1).setText("alex@example.com");
                fields.get(2).setText("secret");
                fields.get(3).setText("different");
                registerButton.fire();
                UiPageTestSupport.assertFeedback(page, "Passwords do not match.");

                fields.get(3).setText("secret");
                registerButton.fire();
                assertEquals(AccountType.MEMBER, context.authenticationService().lastAccountType);
                assertEquals("Alex", context.authenticationService().lastName);
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void librarianAndRepositoryFailures_showExpectedMessages() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                RegistrationPage page = new RegistrationPage(context.authenticationService(),
                        context.navigator());
                ComboBox<?> selector = UiTestSupport.findNodes(page, ComboBox.class).get(0);
                selector.getSelectionModel().select(1);
                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Grace");
                fields.get(1).setText("grace@example.com");
                fields.get(2).setText("secret");
                fields.get(3).setText("secret");
                fields.get(4).setText("e1");
                UiTestSupport.findButton(page, "Register").fire();
                assertEquals(AccountType.LIBRARIAN, context.authenticationService().lastAccountType);
                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }

            UiPageTestSupport.TestContext failureContext = UiPageTestSupport.context();
            try {
                RegistrationPage page = new RegistrationPage(failureContext.authenticationService(),
                        failureContext.navigator());
                ComboBox<?> selector = UiTestSupport.findNodes(page, ComboBox.class).get(0);
                selector.getSelectionModel().select(0);
                failureContext.authenticationService().registerFailure = UiPageTestSupport.repositoryFailure();
                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Alex");
                fields.get(1).setText("alex@example.com");
                fields.get(2).setText("secret");
                fields.get(3).setText("secret");
                UiTestSupport.findButton(page, "Register").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to access account data. Please try again.");
            } finally {
                failureContext.close();
            }
        });
    }

    @Test
    void emptyFieldsAndMissingEmployeeId_showValidationMessages() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                RegistrationPage page = new RegistrationPage(context.authenticationService(),
                        context.navigator());
                ComboBox<?> selector = UiTestSupport.findNodes(page, ComboBox.class).get(0);
                selector.getSelectionModel().select(0);
                UiTestSupport.findButton(page, "Register").fire();
                UiPageTestSupport.assertFeedback(page, "Name, email and password are required.");

                selector.getSelectionModel().select(1);
                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText("Grace");
                fields.get(1).setText("grace@example.com");
                fields.get(2).setText("secret");
                fields.get(3).setText("secret");
                UiTestSupport.findButton(page, "Register").fire();
                UiPageTestSupport.assertFeedback(page,
                        "Employee ID is required for librarian accounts.");
            } finally {
                context.close();
            }
        });
    }

    @Test
    void backToLogin_navigatesWithoutRegistering() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                RegistrationPage page = new RegistrationPage(context.authenticationService(),
                        context.navigator());

                UiTestSupport.findButton(page, "Back to login").fire();

                assertInstanceOf(LoginPage.class, context.stage().getScene().getRoot());
            } finally {
                context.close();
            }
        });
    }
}
