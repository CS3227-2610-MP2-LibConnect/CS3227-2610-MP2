package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests member registration fields and accessors. */
class MemberRegistrationFormTest {
    @Test
    void rendersFieldsAndReturnsEnteredValues() {
        UiTestSupport.runOnFxThread(() -> {
            MemberRegistrationForm form = new MemberRegistrationForm();
            List<TextField> fields = UiTestSupport.findTextFields(form);
            String name = "Name";
            String email = "name@example.com";
            String password = "secret";
            assertEquals(4, fields.size());
            fields.get(0).setText(name);
            fields.get(1).setText(email);
            fields.get(2).setText(password);
            fields.get(3).setText(password);
            assertEquals(name, form.getName());
            assertEquals(email, form.getEmail());
            assertEquals(password, form.getPassword());
            assertEquals(password, form.getPasswordConfirmation());
            assertTrue(fields.get(2) instanceof PasswordField);
        });
    }
}
