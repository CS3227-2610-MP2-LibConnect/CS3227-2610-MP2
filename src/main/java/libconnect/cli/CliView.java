package libconnect.cli;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Objects;

import libconnect.librarian.LibrarianView;

/** Prints controller feedback to the CLI output stream. */
final class CliView implements LibrarianView {
    private final PrintWriter output;

    /** Creates a CLI view backed by the supplied writer. */
    CliView(Writer output) {
        this.output = new PrintWriter(Objects.requireNonNull(output, "output"), true);
    }

    /** Prints an informational message. */
    @Override
    public void showMessage(String message) {
        output.println("OK: " + message);
    }

    /** Prints an error message. */
    @Override
    public void showError(String message) {
        output.println("ERROR: " + message);
    }
}
