package libconnect;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.app.LibrarianCompositionRoot;
import libconnect.app.LibrarianRuntime;
import libconnect.librarian.LibrarianView;

/** Verifies that the shared desktop composition root assembles the librarian runtime. */
class LibrarianCompositionRootTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    @Test
    void create_isolatedDataDirectory_returnsCompleteRuntime() {
        LibrarianRuntime runtime = LibrarianCompositionRoot.create(temporaryDirectory, CLOCK,
                new NoOpView());

        assertNotNull(runtime.librarianService());
        assertNotNull(runtime.reservationService());
        assertNotNull(runtime.fineService());
        assertNotNull(runtime.notificationService());
        assertNotNull(runtime.controller());
    }

    private static final class NoOpView implements LibrarianView {
        @Override
        public void showMessage(String message) {
        }

        @Override
        public void showError(String message) {
        }
    }
}
