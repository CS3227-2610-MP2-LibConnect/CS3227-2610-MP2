package libconnect.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.models.Member;
import libconnect.services.ServiceException;
import libconnect.ui.pages.ProfilePage;

/** Tests profile updates, password changes, validation, and failures. */
class ProfilePageTest {
    @Test
    void updatesProfileAndPasswordOrShowsValidation() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                ProfilePage page = new ProfilePage(context.memberService(), context.sessionManager(),
                        context.navigator());
                List<TextField> fields = UiTestSupport.findTextFields(page);
                fields.get(0).setText(" ");
                UiTestSupport.findButton(page, "Update information").fire();
                UiPageTestSupport.assertFeedback(page, "Name and email are required.");

                fields.get(0).setText("Updated");
                fields.get(1).setText("updated@example.com");
                context.memberService().updatedMember = new Member("USER-1", "Updated",
                        "updated@example.com", "hash", "MEM-1");
                UiTestSupport.findButton(page, "Update information").fire();
                UiPageTestSupport.assertFeedback(page, "Profile updated successfully.");

                fields.get(2).setText("new-secret");
                fields.get(3).setText("different");
                UiTestSupport.findButton(page, "Reset password").fire();
                UiPageTestSupport.assertFeedback(page, "Passwords do not match.");

                fields.get(3).setText("new-secret");
                UiTestSupport.findButton(page, "Reset password").fire();
                UiPageTestSupport.assertFeedback(page, "Password updated successfully.");
                assertEquals("", fields.get(2).getText());
                assertEquals("", fields.get(3).getText());
            } finally {
                context.close();
            }
        });
    }

    @Test
    void serviceAndStorageFailures_showFallbackMessages() {
        UiTestSupport.runOnFxThread(() -> {
            UiPageTestSupport.TestContext context = UiPageTestSupport.context();
            try {
                ProfilePage page = new ProfilePage(context.memberService(), context.sessionManager(),
                        context.navigator());
                List<TextField> fields = UiTestSupport.findTextFields(page);
                context.memberService().profileFailure = new ServiceException("");
                fields.get(0).setText("Updated");
                fields.get(1).setText("updated@example.com");
                UiTestSupport.findButton(page, "Update information").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to update profile. Please try again.");

                context.memberService().passwordFailure = UiPageTestSupport.repositoryFailure();
                fields.get(2).setText("secret");
                fields.get(3).setText("secret");
                UiTestSupport.findButton(page, "Reset password").fire();
                UiPageTestSupport.assertFeedback(page, "Unable to access account data. Please try again.");
            } finally {
                context.close();
            }
        });
    }
}
