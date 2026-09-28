package libconnect.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import libconnect.storage.repositories.Identifiable;

import libconnect.storage.repositories.RepositoryException;

/** Provides safe shared file-system operations for file-backed repositories. */
public final class StorageManager {
    private static final Logger LOGGER = Logger.getLogger(StorageManager.class.getName());
    private static final String MALFORMED_DIRECTORY_NAME = "malformed";
    private static final String MALFORMED_RECORDS_FILE_NAME = "malformed-records.json";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String SOURCE_FILE_FIELD = "sourceFile";
    private static final String ENTITY_TYPE_FIELD = "entityType";
    private static final String RECORD_INDEX_FIELD = "recordIndex";
    private static final String REASON_FIELD = "reason";
    private static final String RAW_RECORD_FIELD = "rawRecord";

    private final Path dataDirectory;
    private final Path malformedRecordsFile;
    private final ObjectMapper objectMapper;

    /** Creates a storage manager rooted at the supplied data directory. */
    public StorageManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.malformedRecordsFile = this.dataDirectory.resolve(MALFORMED_DIRECTORY_NAME)
                .resolve(MALFORMED_RECORDS_FILE_NAME);
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /** Ensures that the data directory and a JSON array file exist. */
    public void ensureDataFile(Path file) {
        try {
            Files.createDirectories(dataDirectory);
            if (Files.notExists(file)) {
                Files.writeString(file, "[]");
            }
        } catch (IOException exception) {
            throw new RepositoryException("Unable to initialize data file " + file, exception);
        }
    }

    /** Reads all entities of the supplied type from a JSON array file. */
    public <T> List<T> readList(Path file, Class<T> entityType) {
        ensureDataFile(file);

        JsonNode root;
        try {
            root = objectMapper.readTree(file.toFile());
        } catch (IOException | RuntimeException exception) {
            throw new RepositoryException("Unable to read data file " + file, exception);
        }

        if (root == null || !root.isArray()) {
            throw new RepositoryException("Unable to read data file " + file
                    + ": root value must be a JSON array",
                    new IllegalArgumentException("Root value must be a JSON array"));
        }

        List<T> entities = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        for (int index = 0; index < root.size(); index++) {
            JsonNode rawRecord = root.get(index);
            try {
                T entity = deserializeRecord(rawRecord, entityType);
                if (entity instanceof Identifiable identifiable
                        && !identifiers.add(identifiable.getId())) {
                    throw new IllegalArgumentException("Duplicate identifier: "
                            + identifiable.getId());
                }
                entities.add(entity);
            } catch (JsonProcessingException | RuntimeException exception) {
                recordMalformedData(file, entityType, index, rawRecord, exception);
            }
        }

        return List.copyOf(entities);
    }

    /** Replaces a JSON array file atomically where the file system supports it. */
    public <T> void writeList(Path file, List<T> entities) {
        ensureDataFile(file);
        try {
            String json = objectMapper.writeValueAsString(entities);
            writeJsonAtomically(file, json);
        } catch (JsonProcessingException exception) {
            throw new RepositoryException("Unable to serialize data for " + file, exception);
        } catch (IOException exception) {
            throw new RepositoryException("Unable to write data file " + file, exception);
        }
    }

    /** Deserializes one JSON record and rejects null or non-object array elements. */
    private <T> T deserializeRecord(JsonNode rawRecord, Class<T> entityType)
            throws JsonProcessingException {
        if (rawRecord == null || !rawRecord.isObject()) {
            throw new IllegalArgumentException("Record must be a JSON object");
        }

        T entity = objectMapper.treeToValue(rawRecord, entityType);
        if (entity == null) {
            throw new IllegalArgumentException("Record must not deserialize to null");
        }
        return entity;
    }

    /** Persists a malformed record in the recovery journal and emits a timestamped warning. */
    private <T> void recordMalformedData(Path sourceFile, Class<T> entityType, int index,
                                         JsonNode rawRecord, Exception exception) {
        Instant timestamp = Instant.now();
        String timestampText = timestamp.toString();
        String reason = exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();

        try {
            appendMalformedRecord(timestampText, sourceFile, entityType, index, reason, rawRecord);
        } catch (RepositoryException journalFailure) {
            LOGGER.log(Level.SEVERE, "Unable to persist malformed record found at "
                    + timestampText + " from " + sourceFile, journalFailure);
            throw journalFailure;
        }

        LOGGER.log(Level.WARNING, "Skipped malformed " + entityType.getSimpleName()
                + " record #" + (index + 1) + " from " + sourceFile + " at " + timestampText
                + "; recovery entry written to " + malformedRecordsFile + ": " + reason,
                exception);
    }

    /** Appends one malformed record entry to the recovery journal. */
    private <T> void appendMalformedRecord(String timestamp, Path sourceFile, Class<T> entityType,
                                           int index, String reason, JsonNode rawRecord) {
        try {
            ArrayNode entries = readMalformedRecords();
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put(TIMESTAMP_FIELD, timestamp);
            entry.put(SOURCE_FILE_FIELD, sourceFile.toAbsolutePath().normalize().toString());
            entry.put(ENTITY_TYPE_FIELD, entityType.getName());
            entry.put(RECORD_INDEX_FIELD, index + 1);
            entry.put(REASON_FIELD, reason);
            entry.set(RAW_RECORD_FIELD, rawRecord == null
                    ? objectMapper.nullNode() : rawRecord.deepCopy());
            entries.add(entry);
            writeJsonAtomically(malformedRecordsFile, objectMapper.writeValueAsString(entries));
        } catch (IOException | RuntimeException exception) {
            throw new RepositoryException("Unable to persist malformed record in "
                    + malformedRecordsFile, exception);
        }
    }

    /** Reads the existing malformed-record journal or creates an empty journal in memory. */
    private ArrayNode readMalformedRecords() throws IOException {
        if (Files.notExists(malformedRecordsFile)) {
            return objectMapper.createArrayNode();
        }

        JsonNode existingEntries = objectMapper.readTree(malformedRecordsFile.toFile());
        if (existingEntries == null || !existingEntries.isArray()) {
            throw new IOException("Malformed-record journal root must be a JSON array");
        }
        return (ArrayNode) existingEntries;
    }

    /** Writes JSON through a temporary file and replaces the destination after a complete write. */
    private void writeJsonAtomically(Path file, String json) throws IOException {
        Path destination = file.toAbsolutePath().normalize();
        Path parentDirectory = destination.getParent() == null
                ? dataDirectory : destination.getParent();
        Files.createDirectories(parentDirectory);

        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(dataDirectory,
                    destination.getFileName().toString(), ".tmp");
            Files.writeString(temporaryFile, json);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // The original file is already safe; a leftover temporary file is recoverable.
                }
            }
        }
    }
}
