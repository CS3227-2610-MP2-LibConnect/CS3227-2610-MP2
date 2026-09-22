package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.storage.exceptions.DeleteFailureException;

public interface Repository<T> {
    Optional<T> findById(String id);

    boolean deleteById(String id) throws DeleteFailureException;

    void save(T entity);

    List<T> findAll();

}
