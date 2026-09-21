package libconnect.storage.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import libconnect.storage.FileManager;
import libconnect.storage.exceptions.DeleteFailureException;
/**
 * Provides shared file and collection operations for file-backed repositories.
 */
abstract class FileRepositorySupport {
    private static final Logger LOGGER = Logger.getLogger(FileRepositorySupport.class.getName());
    protected final FileManager fileManager;
    protected final Path dataFile;
    private final String entityDescription;

    /**
     * Creates shared support for a repository data file.
     *
     * @param fileManager the manager used for file operations.
     * @param dataFile the file used for persistence.
     * @param entityDescription the entity name used in error messages.
     * @throws IllegalStateException if the data file cannot be created.
     */
    protected FileRepositorySupport(FileManager fileManager, Path dataFile,
                                    String entityDescription) {
        this.fileManager = Objects.requireNonNull(fileManager, "fileManager cannot be null");
        this.dataFile = Objects.requireNonNull(dataFile, "dataFile cannot be null");
        this.entityDescription = Objects.requireNonNull(entityDescription,
                "entityDescription cannot be null");
        ensureDataFileExists();
    }

    /**
     * Reads and parses all records from the repository data file.
     *
     * @param parser the parser for the stored JSON content.
     * @param <T> the record type.
     * @return the parsed records, or an empty list when no file content exists or the stored
     *         JSON cannot be parsed.
     * @throws IllegalStateException if the file cannot be read.
     */
    protected <T> List<T> readRecords(Function<String, List<T>> parser) {
        try {
            Optional<String> storedData = fileManager.read(dataFile);
            return storedData.map(parser).orElseGet(ArrayList::new);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(Level.WARNING, "Unable to parse " + entityDescription + " data from "
                    + dataFile + "; returning no records", exception);
            return new ArrayList<>();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + entityDescription
                    + " from " + dataFile, exception);
        }
    }

    /**
     * Logs a malformed record without interrupting repository reads.
     *
     * @param logger the logger associated with the repository.
     * @param recordType the type of record being skipped.
     * @param message the reason the record is malformed.
     * @param exception the parsing exception, if available.
     */
    protected static void logMalformedRecord(Logger logger, String recordType,
                                             String message, Throwable exception) {
        logger.log(Level.WARNING, "Skipping malformed " + recordType + " record: " + message,
                exception);
    }

    /**
     * Reads all records as an immutable result list.
     *
     * @param parser the parser for the stored JSON content.
     * @param <T> the record type.
     * @return all parsed records.
     */
    protected <T> List<T> readAllRecords(Function<String, List<T>> parser) {
        return List.copyOf(readRecords(parser));
    }

    /**
     * Finds the first record matching a predicate.
     *
     * @param parser the parser for the stored JSON content.
     * @param predicate the matching condition.
     * @param <T> the record type.
     * @return the first matching record, or an empty optional.
     */
    protected <T> Optional<T> findFirst(Function<String, List<T>> parser,
                                        Predicate<T> predicate) {
        return readRecords(parser).stream()
                .filter(predicate)
                .findFirst();
    }

    /**
     * Finds all records matching a predicate.
     *
     * @param parser the parser for the stored JSON content.
     * @param predicate the matching condition.
     * @param <T> the record type.
     * @return all matching records.
     */
    protected <T> List<T> findMatching(Function<String, List<T>> parser,
                                        Predicate<T> predicate) {
        return readRecords(parser).stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Serializes and atomically writes all records to the repository data file.
     *
     * @param records the records to persist.
     * @param serializer the serializer for the records.
     * @param <T> the record type.
     * @throws IllegalStateException if the file cannot be written.
     */
    protected <T> void writeRecords(List<T> records, Function<List<T>, String> serializer) {
        try {
            fileManager.writeAtomically(dataFile, serializer.apply(records));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write " + entityDescription
                    + " to " + dataFile, exception);
        }
    }

    /**
     * Finds the index of the first record matching a predicate.
     *
     * @param records the records to search.
     * @param predicate the matching condition.
     * @param <T> the record type.
     * @return the matching index, or -1 when no record matches.
     */
    protected <T> int findIndex(List<T> records, Predicate<T> predicate) {
        for (int index = 0; index < records.size(); index++) {
            if (predicate.test(records.get(index))) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Replaces the first matching record or appends the supplied record.
     *
     * @param records the records to update.
     * @param record the record to insert or replace.
     * @param predicate the condition identifying an existing record.
     * @param <T> the record type.
     */
    protected <T> void upsert(List<T> records, T record, Predicate<T> predicate) {
        int existingIndex = findIndex(records, predicate);
        if (existingIndex >= 0) {
            records.set(existingIndex, record);
        } else {
            records.add(record);
        }
    }

    /**
     * Removes all records matching a predicate.
     *
     * @param records the records to update.
     * @param predicate the condition identifying records to remove.
     * @param <T> the record type.
     * @return true if at least one record was removed.
     */
    protected <T> boolean removeMatching(List<T> records, Predicate<T> predicate) {
        return records.removeIf(predicate);
    }

    /**
     * Removes matching records and writes the updated collection.
     *
     * @param records the records to update.
     * @param predicate the condition identifying records to remove.
     * @param serializer the serializer for the updated records.
     * @param failureMessage the message used when no record is removed.
     * @param <T> the record type.
     * @throws DeleteFailureException if no record matches the predicate.
     */
    protected <T> void deleteMatching(List<T> records, Predicate<T> predicate,
                                      Function<List<T>, String> serializer,
                                      String failureMessage) throws DeleteFailureException {
        if (!removeMatching(records, predicate)) {
            throw new DeleteFailureException(failureMessage);
        }

        writeRecords(records, serializer);
    }

    /**
     * Ensures that the repository data file exists.
     */
    private void ensureDataFileExists() {
        try {
            fileManager.ensureFileExists(dataFile);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create " + entityDescription
                    + " file at " + dataFile, exception);
        }
    }
}
