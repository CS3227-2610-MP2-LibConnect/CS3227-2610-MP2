package libconnect.librarian;

/** Defines the presentation boundary for librarian-facing controllers. */
public interface LibrarianView {
    /** Displays a user-facing success or information message. */
    void showMessage(String message);

    /** Displays a user-facing error message. */
    void showError(String message);
}
