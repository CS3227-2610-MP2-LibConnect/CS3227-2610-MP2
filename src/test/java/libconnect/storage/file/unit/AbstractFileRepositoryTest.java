package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import libconnect.storage.repositories.RepositoryException;

/**
 * Defines unit-test contract cases shared by file-backed repositories.
 *
 * @param <T> the persisted entity type.
 * @param <R> the repository type.
 */
abstract class AbstractFileRepositoryTest<T, R> {
    @TempDir
    protected Path temporaryDirectory;

    /**
     * Creates a repository backed by the supplied data file.
     *
     * @param dataFile the temporary data file.
     * @return the repository under test.
     */
    protected abstract R createRepository(Path dataFile);

    /**
     * Saves an entity through the repository.
     *
     * @param repository the repository under test.
     * @param entity the entity to save.
     */
    protected abstract void save(R repository, T entity);

    /**
     * Returns all entities through the repository.
     *
     * @param repository the repository under test.
     * @return all persisted entities.
     */
    protected abstract List<T> findAll(R repository);

    /**
     * Returns the stable identifier of an entity.
     *
     * @param entity the entity whose identifier is needed.
     * @return the entity identifier.
     */
    protected abstract String getIdentifier(T entity);

    /**
     * Creates a representative entity for the supplied identifier.
     *
     * @param identifier the entity identifier.
     * @param variant a value used to distinguish test entities.
     * @return the test entity.
     */
    protected abstract T createEntity(String identifier, String variant);

    /**
     * Captures warning messages emitted by a repository logger during a test.
     *
     * @param logger the logger whose warnings should be captured.
     * @return a closeable warning capture.
     */
    protected static WarningCapture captureWarnings(Logger logger) {
        return new WarningCapture(logger);
    }

    @Test
    void newRepository_createsDataFileAndStartsEmpty() {
        Path dataFile = temporaryDirectory.resolve("records.json");

        R repository = createRepository(dataFile);

        assertTrue(findAll(repository).isEmpty());
        assertTrue(Files.isRegularFile(dataFile));
    }

    @Test
    void save_newEntity_persistsEntity() {
        Path dataFile = temporaryDirectory.resolve("records.json");
        R repository = createRepository(dataFile);
        T entity = createEntity("ID-1", "first");

        save(repository, entity);

        List<T> entities = findAll(repository);
        assertEquals(1, entities.size());
        assertEquals(getIdentifier(entity), getIdentifier(entities.get(0)));
    }

    @Test
    void save_sameIdentifier_replacesEntityWithoutAddingDuplicate() {
        Path dataFile = temporaryDirectory.resolve("records.json");
        R repository = createRepository(dataFile);
        T originalEntity = createEntity("ID-1", "original");
        T replacementEntity = createEntity("ID-1", "replacement");

        save(repository, originalEntity);
        save(repository, replacementEntity);

        List<T> entities = findAll(repository);
        assertEquals(1, entities.size());
        assertEquals(getIdentifier(replacementEntity), getIdentifier(entities.get(0)));
    }

    @Test
    void save_entityCanBeReadByNewRepositoryInstance() {
        Path dataFile = temporaryDirectory.resolve("records.json");
        T entity = createEntity("ID-1", "first");
        R writingRepository = createRepository(dataFile);

        save(writingRepository, entity);

        R readingRepository = createRepository(dataFile);
        List<T> entities = findAll(readingRepository);
        assertEquals(1, entities.size());
        assertEquals(getIdentifier(entity), getIdentifier(entities.get(0)));
    }

    @Test
    void findAll_malformedStoredData_throwsRepositoryException() throws IOException {
        Path dataFile = temporaryDirectory.resolve("records.json");
        R repository = createRepository(dataFile);
        Files.writeString(dataFile, "not valid json");

        assertThrows(RepositoryException.class, () -> findAll(repository));
    }

    /** Captures log messages from the shared storage logger and removes itself when closed. */
    protected static final class WarningCapture extends Handler implements AutoCloseable {
        private final Logger logger;
        private final List<String> messages = new java.util.ArrayList<>();

        private WarningCapture(Logger logger) {
            this.logger = logger;
            setLevel(Level.ALL);
            logger.addHandler(this);
        }

        /**
         * Returns whether a captured warning contains the supplied text.
         *
         * @param text the text to search for.
         * @return true if a warning contains the text.
         */
        boolean containsMessage(String text) {
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
            // Nothing to flush because records are kept in memory.
        }

        @Override
        public void close() {
            logger.removeHandler(this);
        }
    }
}
