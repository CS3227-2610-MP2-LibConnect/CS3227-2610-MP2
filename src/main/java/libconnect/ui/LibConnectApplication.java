package libconnect.ui;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Starts the LibConnect desktop application.
 */
public final class LibConnectApplication {
    private LibConnectApplication() {
        // Prevent instantiation of the application entry-point class.
    }

    /**
     * Starts the LibConnect user interface on the Swing event-dispatch thread.
     *
     * @param arguments command-line arguments supplied to the application.
     */
    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(LibConnectApplication::createAndShowWindow);
    }

    /**
     * Creates and displays the initial application window.
     */
    private static void createAndShowWindow() {
        JFrame applicationWindow = new JFrame("LibConnect");
        applicationWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applicationWindow.setSize(800, 600);
        applicationWindow.setLocationRelativeTo(null);
        applicationWindow.setVisible(true);
    }
}
