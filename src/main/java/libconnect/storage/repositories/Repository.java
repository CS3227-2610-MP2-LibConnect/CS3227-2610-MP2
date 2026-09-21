package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

/** Defines persistence operations for one model type. */
public interface Repository<T extends Identifiable> {
    /** Finds an entity by its stable identifier. */
    Optional<T> findById(String id);

    /** Returns a fresh snapshot of all persisted entities. */
    List<T> findAll();

    /** Creates or replaces an entity by stable identifier. */
    void save(T entity);

    /** Deletes an entity and returns whether it existed. */
    boolean deleteById(String id);
}
