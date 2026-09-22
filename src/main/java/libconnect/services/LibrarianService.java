package libconnect.services;

import java.util.Optional;

import libconnect.models.User;
import libconnect.util.ValidationUtils;

// TODO: Implement a librarian model and persistence service, then remove this placeholder and delegate to the real service.
/** Provides a temporary librarian-service seam until the librarian model is implemented. */
public class LibrarianService {
    /**
     * Looks up a librarian by user ID.
     *
     * <p>This placeholder always returns an empty result because librarian persistence has not
     * been implemented yet.</p>
     *
     * @param userId the user ID to find.
     * @return always an empty optional until librarian persistence is implemented.
     * @throws IllegalArgumentException if {@code userId} is blank.
     */
    public Optional<User> findByUserId(String userId) {
        ValidationUtils.requireNonBlank(userId, "userId");
        return Optional.empty();
    }

    /**
     * Looks up a librarian by email address.
     *
     * <p>This placeholder always returns an empty result because librarian persistence has not
     * been implemented yet.</p>
     *
     * @param email the email address to find.
     * @return always an empty optional until librarian persistence is implemented.
     * @throws IllegalArgumentException if {@code email} is blank.
     */
    public Optional<User> findByEmail(String email) {
        ValidationUtils.requireNonBlank(email, "email");
        return Optional.empty();
    }
}
