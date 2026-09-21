package libconnect.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import libconnect.storage.repositories.RepositoryException;

/** Provides safe shared file-system operations for file-backed repositories. */
public final class StorageManager {
    private final Path dataDirectory;
    private final ObjectMapper objectMapper;

    /** Creates a storage manager rooted at the supplied data directory. */
    public StorageManager(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
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
        JavaType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, entityType);
        try {
            List<T> entities = objectMapper.readValue(file.toFile(), listType);
            return entities == null ? List.of() : List.copyOf(entities);
        } catch (IOException | RuntimeException exception) {
            throw new RepositoryException("Unable to read data file " + file, exception);
        }
    }

    /** Replaces a JSON array file atomically where the file system supports it. */
    public <T> void writeList(Path file, List<T> entities) {
        ensureDataFile(file);
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(dataDirectory, file.getFileName().toString(), ".tmp");
            String json = objectMapper.writeValueAsString(entities);
            Files.writeString(temporaryFile, json);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicMoveFailure) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (JsonProcessingException exception) {
            throw new RepositoryException("Unable to serialize data for " + file, exception);
        } catch (IOException exception) {
            throw new RepositoryException("Unable to write data file " + file, exception);
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
