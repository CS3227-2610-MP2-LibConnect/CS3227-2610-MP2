package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.Loan;
import libconnect.storage.file.FileLoanRepository;

/** Tests unit-level malformed-record handling for persisted loans. */
class FileLoanRepositoryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void findAll_invalidRecord_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("loans.json");
        FileLoanRepository repository = new FileLoanRepository(dataFile);
        String loans = "[{\"loanId\":\"LOAN-1\",\"memberId\":\"MEMBER-1\","
                + "\"copyId\":\"COPY-1\",\"borrowDate\":\"2025-01-01\","
                + "\"dueDate\":\"2025-01-31\",\"returnDate\":null,"
                + "\"status\":\"INVALID\",\"isRenewed\":false},"
                + "{\"loanId\":\"LOAN-2\",\"memberId\":\"MEMBER-2\","
                + "\"copyId\":\"COPY-2\",\"borrowDate\":\"2025-02-01\","
                + "\"dueDate\":\"2025-03-03\",\"returnDate\":null,"
                + "\"status\":\"ACTIVE\",\"isRenewed\":false}]";
        Files.writeString(dataFile, loans);

        try (AbstractFileRepositoryTest.WarningCapture warnings =
                AbstractFileRepositoryTest.captureWarnings(
                        Logger.getLogger(FileLoanRepository.class.getName()))) {
            assertEquals(List.of(new Loan("LOAN-2", "MEMBER-2", "COPY-2",
                    LocalDate.of(2025, 2, 1))), repository.findAll());
            assertTrue(warnings.containsMessage("Skipping malformed loan record"));
        }
    }
}
