package libconnect.ui.components;

import javafx.scene.control.Label;

/** Displays a hidden feedback message that can represent either an error or a success. */
public final class FeedbackMessage extends Label {
    private static final String ERROR_STYLE = "-fx-text-fill: #b00020;";
    private static final String SUCCESS_STYLE = "-fx-text-fill: #1b5e20;"
            + "-fx-background-color: #e8f5e9;";

    /** Creates an initially hidden feedback message. */
    public FeedbackMessage() {
        setManaged(false);
        setVisible(false);
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Displays an error message.
     *
     * @param message the error message to display.
     */
    public void showError(String message) {
        showMessage(message, ERROR_STYLE);
    }

    /**
     * Displays a success message.
     *
     * @param message the success message to display.
     */
    public void showSuccess(String message) {
        showMessage(message, SUCCESS_STYLE);
    }

    /** Hides and clears the current feedback message. */
    public void clearMessage() {
        setText("");
        setManaged(false);
        setVisible(false);
    }

    private void showMessage(String message, String style) {
        setText(message);
        setStyle(style);
        setManaged(true);
        setVisible(true);
    }
}
