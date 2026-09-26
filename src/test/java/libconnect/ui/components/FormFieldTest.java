package libconnect.ui.components;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.junit.jupiter.api.Test;

import libconnect.ui.UiTestSupport;

/** Tests form-field label and control composition. */
class FormFieldTest {
    @Test
    void preservesLabelAndControl() {
        UiTestSupport.runOnFxThread(() -> {
            TextField input = new TextField();
            FormField formField = new FormField("Name", input);
            assertEquals("Name", ((Label) formField.getChildren().get(0)).getText());
            assertSame(input, formField.getChildren().get(1));
        });
    }
}
