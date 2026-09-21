package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.User;
import libconnect.storage.file.FileUserRepository;

/** Tests unit-level malformed-record handling for persisted users. */
class FileUserRepositoryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void findAll_invalidRecord_logsAndSkipsMalformedRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("users.json");
        FileUserRepository repository = new FileUserRepository(dataFile);
        String users = "[{\"userId\":\"USER-1\",\"role\":\"ADMIN\","
                + "\"name\":\"Invalid\",\"email\":\"invalid@example.com\","
                + "\"passwordHash\":\"hash\",\"status\":\"ACTIVE\"},"
                + "{\"userId\":\"USER-2\",\"role\":\"USER\","
                + "\"name\":\"Valid\",\"email\":\"valid@example.com\","
                + "\"passwordHash\":\"hash\",\"status\":\"ACTIVE\"}]";
        Files.writeString(dataFile, users);

        try (AbstractFileRepositoryTest.WarningCapture warnings =
                AbstractFileRepositoryTest.captureWarnings(
                        Logger.getLogger(FileUserRepository.class.getName()))) {
            assertEquals(List.of(new User("USER-2", "Valid", "valid@example.com", "hash")),
                    repository.findAll());
            assertTrue(warnings.containsMessage("Skipping malformed user record"));
        }
    }
}
