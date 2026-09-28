package libconnect.ui.components;

import javafx.scene.control.Label;

/** Displays a hidden feedback message that can represent either an error or a success. */
public final class FeedbackMessage extends Label {
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
        showMessage(message, "feedback-error");
    }

    /**
     * Displays a success message.
     *
     * @param message the success message to display.
     */
    public void showSuccess(String message) {
        showMessage(message, "feedback-success");
    }

    /** Hides and clears the current feedback message. */
    public void clearMessage() {
        setText("");
        getStyleClass().removeAll("feedback-error", "feedback-success");
        setManaged(false);
        setVisible(false);
    }

    private void showMessage(String message, String styleClass) {
        setText(message);
        getStyleClass().removeAll("feedback-error", "feedback-success");
        getStyleClass().add(styleClass);
        setManaged(true);
        setVisible(true);
    }
}
