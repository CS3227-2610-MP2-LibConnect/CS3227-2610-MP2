package libconnect.gui;

import java.util.Objects;

import javafx.application.Platform;
import javafx.scene.control.Label;
import libconnect.librarian.LibrarianView;

/** Displays controller messages in the active JavaFX shell. */
public final class JavaFxLibrarianView implements LibrarianView {
    private Label statusLabel;

    /** Binds controller messages to the supplied status label. */
    public void bind(Label newStatusLabel) {
        statusLabel = Objects.requireNonNull(newStatusLabel, "newStatusLabel");
    }

    /** Displays a success or information message. */
    @Override
    public void showMessage(String message) {
        updateStatus(Objects.requireNonNull(message, "message"), false);
    }

    /** Displays an error message. */
    @Override
    public void showError(String message) {
        updateStatus(Objects.requireNonNull(message, "message"), true);
    }

    private void updateStatus(String message, boolean isError) {
        if (statusLabel == null) {
            return;
        }
        Runnable update = () -> {
            statusLabel.setText(message);
            statusLabel.setStyle(isError ? "-fx-text-fill: #b00020;" : "-fx-text-fill: #176b2c;");
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }
}
