package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.User;

/**
 * Defines persistence operations for all user accounts.
 *
 * <p>Implementations must enforce uniqueness of user IDs and normalized email addresses across
 * every supported user role.</p>
 */
public interface UserRepository {
    /**
     * Finds a user by the user's stable identifier.
     *
     * @param userId the user identifier to find.
     * @return the matching user, or an empty optional if no user exists.
     */
    Optional<User> findByUserId(String userId);

    /**
     * Finds a user by email address, ignoring differences in case and surrounding whitespace.
     *
     * @param email the email address to find.
     * @return the matching user, or an empty optional if no user exists.
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns every persisted user account.
     *
     * @return all persisted users.
     */
    List<User> findAll();

    /**
     * Inserts a user or replaces the existing user with the same user ID.
     *
     * @param user the user to persist.
     * @throws IllegalArgumentException if the user conflicts with another user's ID or email.
     */
    void save(User user);
}
