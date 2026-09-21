package libconnect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.cli.CliApplication;
import libconnect.cli.LibrarianCli;

/** Tests scripted CLI workflows against isolated librarian persistence. */
class LibrarianCliTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-21T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path temporaryDirectory;

    /** Verifies that a scripted session can exercise the main librarian services. */
    @Test
    void run_scriptedLibrarianWorkflow_printsExpectedResults() {
        String commands = "seed\n"
                + "login e1\n"
                + "members search ada\n"
                + "books search java\n"
                + "loans overdue\n"
                + "fines create loan-1\n"
                + "fines list m1\n"
                + "alerts overdue loan-1\n"
                + "notifications list m1\n"
                + "exit\n";
        StringWriter output = new StringWriter();

        LibrarianCli cli = CliApplication.create(temporaryDirectory, new StringReader(commands), output, CLOCK);
        cli.run();

        String result = output.toString();
        assertTrue(result.contains("OK: Demo data ready"));
        assertTrue(result.contains("OK: Logged in as Demo Librarian"));
        assertTrue(result.contains("Ada ada@example.com active=true"));
        assertTrue(result.contains("Java Fundamentals available=0"));
        assertTrue(result.contains("loan-1 member=m1"));
        assertTrue(result.contains("amount=4.50"));
        assertTrue(result.contains("OVERDUE_ALERT"));
    }

    /** Verifies that protected commands reject unauthenticated users. */
    @Test
    void run_memberCommandBeforeLogin_printsLoginError() {
        StringWriter output = new StringWriter();
        LibrarianCli cli = CliApplication.create(temporaryDirectory,
                new StringReader("members search ada\nexit\n"), output, CLOCK);

        cli.run();

        assertTrue(output.toString().contains("ERROR: Login required"));
    }

    /** Verifies that a seeded librarian persists across CLI sessions. */
    @Test
    void run_seedThenNewSession_canLoginWithPersistedLibrarian(@TempDir Path dataDirectory) {
        StringWriter seedOutput = new StringWriter();
        CliApplication.create(dataDirectory, new StringReader("seed\nexit\n"), seedOutput, CLOCK).run();

        StringWriter loginOutput = new StringWriter();
        CliApplication.create(dataDirectory, new StringReader("login e1\nexit\n"), loginOutput, CLOCK).run();

        assertTrue(loginOutput.toString().contains("OK: Logged in as Demo Librarian"));
    }
}
