package libconnect.storage.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.models.BookCopy;
import libconnect.storage.StorageManager;

/** Tests record-level recovery behavior for JSON-backed storage. */
class StorageManagerTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TempDir
    private Path temporaryDirectory;

    @Test
    void readList_malformedRecord_skipsAndJournalsRecord() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        Files.writeString(dataFile, "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\","
                + "\"status\":\"INVALID\",\"shelfLocation\":\"A1-01\"},"
                + "{\"copyId\":\"COPY-2\",\"isbn\":\"ISBN-2\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-02\"}]");
        StorageManager storageManager = new StorageManager(temporaryDirectory);

        try (WarningCapture warnings = new WarningCapture(StorageManager.class.getName())) {
            List<BookCopy> copies = storageManager.readList(dataFile, BookCopy.class);

            assertEquals(List.of(new BookCopy("COPY-2", "ISBN-2", "A1-02")), copies);
            assertTrue(warnings.contains("Skipped malformed BookCopy record #1"));
        }

        Path journalFile = temporaryDirectory.resolve("malformed/malformed-records.json");
        JsonNode journal = OBJECT_MAPPER.readTree(Files.readString(journalFile));
        assertEquals(1, journal.size());
        JsonNode entry = journal.get(0);
        assertEquals(dataFile.toAbsolutePath().normalize().toString(),
                entry.get("sourceFile").asText());
        assertEquals(BookCopy.class.getName(), entry.get("entityType").asText());
        assertEquals(1, entry.get("recordIndex").asInt());
        assertEquals("INVALID", entry.get("rawRecord").get("status").asText());
        Instant.parse(entry.get("timestamp").asText());
    }

    @Test
    void readList_multipleMalformedRecords_appendsAllJournalEntries() throws IOException {
        Path dataFile = temporaryDirectory.resolve("book-copies.json");
        Files.writeString(dataFile, "[{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-1\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-01\"},"
                + "{\"copyId\":\"COPY-1\",\"isbn\":\"ISBN-2\","
                + "\"status\":\"AVAILABLE\",\"shelfLocation\":\"A1-02\"},"
                + "{\"copyId\":\"COPY-3\",\"isbn\":\"ISBN-3\","
                + "\"status\":\"INVALID\",\"shelfLocation\":\"A1-03\"}]");
        StorageManager storageManager = new StorageManager(temporaryDirectory);

        assertEquals(List.of(new BookCopy("COPY-1", "ISBN-1", "A1-01")),
                storageManager.readList(dataFile, BookCopy.class));

        Path journalFile = temporaryDirectory.resolve("malformed/malformed-records.json");
        JsonNode journal = OBJECT_MAPPER.readTree(Files.readString(journalFile));
        assertEquals(2, journal.size());
        assertEquals(2, journal.get(0).get("recordIndex").asInt());
        assertEquals(3, journal.get(1).get("recordIndex").asInt());
    }

    /** Captures warning messages emitted by a logger. */
    private static final class WarningCapture extends Handler implements AutoCloseable {
        private final Logger logger;
        private final List<String> messages = new java.util.ArrayList<>();

        private WarningCapture(String loggerName) {
            logger = Logger.getLogger(loggerName);
            setLevel(Level.ALL);
            logger.addHandler(this);
        }

        /** Returns whether a warning message contains the supplied text. */
        private boolean contains(String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
            // Messages are kept in memory for assertions.
        }

        @Override
        public void close() {
            logger.removeHandler(this);
        }
    }
}
