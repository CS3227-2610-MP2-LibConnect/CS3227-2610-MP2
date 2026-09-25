package libconnect;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Test
    void run_helpAndMalformedCommands_printUsefulErrors() {
        StringWriter output = new StringWriter();
        LibrarianCli cli = CliApplication.create(temporaryDirectory,
                new StringReader("help\nunknown\nlogin\nbooks add\nexit\n"), output, CLOCK);

        cli.run();

        String result = output.toString();
        assertTrue(result.contains("members search"));
        assertTrue(result.contains("ERROR: Unknown command; type 'help'"));
        assertTrue(result.contains("ERROR: Usage: login <employeeId>"));
        assertTrue(result.contains("ERROR: Login required"));
    }

    @Test
    void run_memberBookLoanAndCopyCommands_coverManagementFeatures() {
        String commands = "seed\n"
                + "login e1\n"
                + "members register m2 Grace grace@example.com\n"
                + "members edit m2 GraceUpdated updated@example.com\n"
                + "members search grace\n"
                + "members deactivate m2\n"
                + "books add isbn2 Python Author Publisher Programming 2024\n"
                + "books edit isbn2 isbn2 PythonUpdated Author Publisher Programming 2024\n"
                + "books search pythonupdated\n"
                + "books damaged copy-2\n"
                + "books lost copy-3\n"
                + "books remove isbn2\n"
                + "loans list\n"
                + "loans overdue\n"
                + "exit\n";

        String result = run(commands);

        assertTrue(result.contains("OK: Member registered"));
        assertTrue(result.contains("m2 GraceUpdated updated@example.com active=true"));
        assertTrue(result.contains("OK: Member deactivated"));
        assertTrue(result.contains("OK: Book added with ID isbn2"));
        assertTrue(result.contains("isbn2 PythonUpdated available=0"));
        assertTrue(result.contains("OK: Copy recorded as damaged"));
        assertTrue(result.contains("OK: Copy recorded as lost"));
        assertTrue(result.contains("OK: Book removed"));
        assertTrue(result.contains("loan-1 member=m1"));
    }

    @Test
    void run_reservationCommands_supportReminderCancellationAndFulfilment() {
        String createOutput = run("seed\nlogin e1\nreservations create m1 b1\nexit\n");
        String reservationId = extract(createOutput, "OK: Reservation created with ID (\\S+)");

        String reminderAndCancel = run("seed\nlogin e1\n"
                + "reservations list m1\n"
                + "reservations pending b1\n"
                + "alerts reservation " + reservationId + "\n"
                + "reservations cancel " + reservationId + "\nexit\n");
        assertTrue(reminderAndCancel.contains("status=PENDING"));
        assertTrue(reminderAndCancel.contains("OK: Reminder"));
        assertTrue(reminderAndCancel.contains("OK: Reservation cancelled"));

        String secondCreateOutput = run("seed\nlogin e1\nreservations create m1 b1\nexit\n");
        String secondReservationId = extract(secondCreateOutput, "OK: Reservation created with ID (\\S+)");
        String fulfilOutput = run("seed\nlogin e1\nreservations fulfil " + secondReservationId + "\nexit\n");

        assertTrue(fulfilOutput.contains("OK: Reservation fulfilled"));
    }

    @Test
    void run_fineAndNotificationCommands_supportEditRemoveAndRead() {
        String createOutput = run("seed\nlogin e1\n"
                + "fines create loan-1\n"
                + "alerts overdue loan-1\nexit\n");
        String fineId = extract(createOutput, "OK: Fine (\\S+) amount=4\\.50");
        String notificationId = extract(createOutput, "OK: Alert (\\S+)");

        String followUpOutput = run("seed\nlogin e1\n"
                + "fines list m1\n"
                + "fines edit " + fineId + " 3.25\n"
                + "fines remove " + fineId + "\n"
                + "notifications list m1\n"
                + "notifications read " + notificationId + "\nexit\n");

        assertTrue(followUpOutput.contains("amount=4.50"));
        assertTrue(followUpOutput.contains("OK: Fine updated amount=3.25"));
        assertTrue(followUpOutput.contains("OK: Fine removed"));
        assertTrue(followUpOutput.contains("OVERDUE_ALERT"));
        assertTrue(followUpOutput.contains("read=true"));
    }

    @Test
    void run_invalidLoginAndInvalidNumericValue_printErrors() {
        String result = run("login missing\nseed\nlogin e1\n"
                + "books add isbn Title Author Publisher Category not-a-year\nexit\n");

        assertTrue(result.contains("ERROR: Librarian does not exist"));
        assertTrue(result.contains("ERROR: For input string: \"not-a-year\""));
    }

    private String run(String commands) {
        StringWriter output = new StringWriter();
        CliApplication.create(temporaryDirectory, new StringReader(commands), output, CLOCK).run();
        return output.toString();
    }

    private static String extract(String output, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(output);
        assertTrue(matcher.find(), "Expected output to match: " + expression + "\n" + output);
        return matcher.group(1);
    }
}
