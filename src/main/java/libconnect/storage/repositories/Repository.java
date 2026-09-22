package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.storage.exceptions.DeleteFailureException;

/**
 * Defines the common persistence operations supported by an entity repository.
 *
 * @param <T> the type of entity managed by the repository.
 */
public interface Repository<T> {
    /**
     * Finds an entity by its stable identifier.
     *
     * @param id the identifier to find.
     * @return the matching entity, or an empty optional if no entity exists.
     * @throws IllegalArgumentException if {@code id} is null or blank.
     */
    Optional<T> findById(String id);

    /**
     * Deletes an entity by its stable identifier.
     *
     * @param id the identifier of the entity to delete.
     * @return true if an entity was deleted, or false if no matching entity exists.
     * @throws IllegalArgumentException if {@code id} is null or blank.
     * @throws DeleteFailureException if the deletion cannot be completed.
     */
    boolean deleteById(String id) throws DeleteFailureException;

    /**
     * Creates an entity or replaces an existing entity with the same identifier.
     *
     * @param entity the entity to persist.
     * @throws NullPointerException if {@code entity} is null.
     */
    void save(T entity);

    /**
     * Returns a snapshot of all persisted entities.
     *
     * @return all persisted entities.
     */
    List<T> findAll();

}
