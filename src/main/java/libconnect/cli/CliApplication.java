package libconnect.cli;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

import libconnect.app.LibrarianCompositionRoot;
import libconnect.app.LibrarianRuntime;

/** Composes the real librarian services with temporary CLI integration adapters. */
public final class CliApplication {
    private CliApplication() {
    }

    /** Creates a CLI backed by the supplied data directory and I/O streams. */
    public static LibrarianCli create(Path dataDirectory, Reader input, Writer output, Clock clock) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(clock, "clock");

        CliView view = new CliView(output);
        LibrarianRuntime runtime = LibrarianCompositionRoot.create(dataDirectory, clock, view);

        return new LibrarianCli(input, output, runtime.librarianService(), runtime.reservationService(),
                runtime.fineService(), runtime.notificationService(), runtime.controller());
    }
}
