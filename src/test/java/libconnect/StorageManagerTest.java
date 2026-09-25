package libconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.storage.StorageManager;
import libconnect.storage.repositories.RepositoryException;

/** Tests shared JSON storage initialization, serialization, and failure handling. */
class StorageManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void readList_missingFile_createsDirectoryAndReturnsEmptyList() {
        Path dataDirectory = temporaryDirectory.resolve("nested");
        Path file = dataDirectory.resolve("records.json");
        StorageManager storageManager = new StorageManager(dataDirectory);

        assertEquals(List.of(), storageManager.readList(file, String.class));
        assertTrue(Files.exists(file));
    }

    @Test
    void writeList_roundTripsJsonValues() {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        Path file = temporaryDirectory.resolve("records.json");

        storageManager.writeList(file, List.of("one", "two"));

        assertEquals(List.of("one", "two"), storageManager.readList(file, String.class));
    }

    @Test
    void readList_malformedJson_throwsRepositoryException() throws Exception {
        StorageManager storageManager = new StorageManager(temporaryDirectory);
        Path file = temporaryDirectory.resolve("records.json");
        Files.createDirectories(temporaryDirectory);
        Files.writeString(file, "{not-json");

        assertThrows(RepositoryException.class, () -> storageManager.readList(file, String.class));
    }
}
