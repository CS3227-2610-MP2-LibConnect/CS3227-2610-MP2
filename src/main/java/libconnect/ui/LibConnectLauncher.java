package libconnect.ui;

import javafx.application.Application;

public class LibConnectLauncher {
    /**
     * Starts the LibConnect user interface on the JavaFX application thread.
     *
     * @param arguments command-line arguments supplied to the application.
     */
    public static void main(String[] arguments) {
        Application.launch(LibConnectApplication.class, arguments);
    }
}
