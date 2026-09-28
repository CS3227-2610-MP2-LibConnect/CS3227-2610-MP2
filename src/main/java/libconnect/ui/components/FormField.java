package libconnect.ui.components;

import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Combines a form label with its reusable JavaFX input control. */
public final class FormField extends VBox {
    private static final double SPACING = 5;

    /**
     * Creates a labelled form field.
     *
     * @param labelText the text displayed above the input control.
     * @param inputControl the input control used by the field.
     */
    public FormField(String labelText, Control inputControl) {
        Label label = new Label(labelText);
        setSpacing(SPACING);
        getChildren().addAll(label, inputControl);
    }
}
