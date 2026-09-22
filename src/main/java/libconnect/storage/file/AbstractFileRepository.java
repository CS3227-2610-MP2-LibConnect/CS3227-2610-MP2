package libconnect.storage.file;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import libconnect.storage.StorageManager;
import libconnect.storage.repositories.Identifiable;
import libconnect.storage.repositories.Repository;

/** Provides read-through CRUD behavior for a JSON-backed repository. */
public abstract class AbstractFileRepository<T extends Identifiable> implements Repository<T> {
    private final StorageManager storageManager;
    private final Path file;
    private final Class<T> entityType;

    /** Creates a repository backed by one JSON data file. */
    protected AbstractFileRepository(StorageManager storageManager, Path file, Class<T> entityType) {
        this.storageManager = Objects.requireNonNull(storageManager, "storageManager");
        this.file = Objects.requireNonNull(file, "file");
        this.entityType = Objects.requireNonNull(entityType, "entityType");
    }

    /** Returns the backing file path for diagnostics and integration setup. */
    protected final Path getFile() {
        return file;
    }

    /** Returns a fresh list loaded from disk. */
    protected final List<T> readAll() {
        return storageManager.readList(file, entityType);
    }

    /** Returns the entity with the supplied identifier, if present. */
    @Override
    public final Optional<T> findById(String id) {
        String requiredId = requireId(id);
        return readAll().stream().filter(entity -> entity.getId().equals(requiredId)).findFirst();
    }

    /** Returns a fresh snapshot of all entities. */
    @Override
    public final List<T> findAll() {
        return readAll();
    }

    /** Creates or replaces an entity and persists the complete snapshot. */
    @Override
    public final void save(T entity) {
        Objects.requireNonNull(entity, "entity");
        List<T> entities = new ArrayList<>(readAll());
        int existingIndex = findIndex(entities, entity.getId());
        if (existingIndex >= 0) {
            entities.set(existingIndex, entity);
        } else {
            entities.add(entity);
        }
        storageManager.writeList(file, entities);
    }

    /** Deletes an entity by identifier and reports whether it existed. */
    @Override
    public final boolean deleteById(String id) {
        String requiredId = requireId(id);
        List<T> entities = new ArrayList<>(readAll());
        boolean removed = entities.removeIf(entity -> entity.getId().equals(requiredId));
        if (removed) {
            storageManager.writeList(file, entities);
        }
        return removed;
    }

    private int findIndex(List<T> entities, String id) {
        for (int index = 0; index < entities.size(); index++) {
            if (entities.get(index).getId().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }

        return id.trim();
    }
}
