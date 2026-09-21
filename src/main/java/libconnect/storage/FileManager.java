package libconnect.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides shared file-system operations for file-backed repositories.
 */
public final class FileManager {
    /**
     * Creates the supplied data file and its parent directories when they do not exist.
     *
     * @param dataFile the file to ensure exists.
     * @throws IOException if the parent directories or data file cannot be created.
     * @throws NullPointerException if {@code dataFile} is null.
     */
    public void ensureFileExists(Path dataFile) throws IOException {
        Path requiredDataFile = requireDataFile(dataFile).toAbsolutePath();
        Path parentDirectory = requiredDataFile.getParent();

        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }

        if (Files.exists(requiredDataFile) && !Files.isRegularFile(requiredDataFile)) {
            throw new IOException("Data path is not a regular file: " + dataFile);
        }

        try {
            Files.createFile(requiredDataFile);
            Files.writeString(requiredDataFile, "[]\n", StandardCharsets.UTF_8);
        } catch (FileAlreadyExistsException exception) {
            // The file was created by another operation between the existence check and creation.
        }
    }

    /**
     * Reads a UTF-8 file when it exists.
     *
     * @param dataFile the file to read.
     * @return the file contents, or an empty optional when the file does not exist.
     * @throws IOException if the file cannot be read.
     */
    public Optional<String> read(Path dataFile) throws IOException {
        Path requiredDataFile = requireDataFile(dataFile);
        if (!Files.exists(requiredDataFile)) {
            return Optional.empty();
        }

        return Optional.of(Files.readString(requiredDataFile, StandardCharsets.UTF_8));
    }

    /**
     * Writes UTF-8 content by replacing the destination with a completed temporary file.
     *
     * @param dataFile the file to replace.
     * @param content the content to write.
     * @throws IOException if the file cannot be written or replaced.
     */
    public void writeAtomically(Path dataFile, String content) throws IOException {
        Path requiredDataFile = requireDataFile(dataFile);
        Objects.requireNonNull(content, "content cannot be null");

        Path absoluteDataFile = requiredDataFile.toAbsolutePath();
        Path parentDirectory = absoluteDataFile.getParent();
        Path temporaryFile = null;

        try {
            Files.createDirectories(parentDirectory);
            temporaryFile = Files.createTempFile(parentDirectory, "libconnect-", ".tmp");
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            replaceDataFile(temporaryFile, absoluteDataFile);
            temporaryFile = null;
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    /**
     * Validates a file path supplied to the file manager.
     *
     * @param dataFile the file path to validate.
     * @return the validated file path.
     * @throws NullPointerException if {@code dataFile} is null.
     */
    private Path requireDataFile(Path dataFile) {
        return Objects.requireNonNull(dataFile, "dataFile cannot be null");
    }

    /**
     * Replaces the destination with the completed temporary file.
     *
     * @param temporaryFile the completed temporary file.
     * @param destinationFile the persistent data file.
     * @throws IOException if the replacement fails.
     */
    private void replaceDataFile(Path temporaryFile, Path destinationFile) throws IOException {
        try {
            Files.move(temporaryFile, destinationFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Deletes a temporary file after a failed write.
     *
     * @param temporaryFile the temporary file to delete, or null when no cleanup is needed.
     */
    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException exception) {
            // The original write failure is more useful to the caller than cleanup failure.
        }
    }
}
