package libconnect.services;

import java.util.Objects;
import java.util.UUID;

import libconnect.storage.repositories.MemberRepository;

/** Generates user IDs that are unique across member and librarian accounts. */
public final class UserIdGenerator {
    private final UserService userService;

    /**
     * Creates a generator backed by the member repository and librarian service.
     *
     * @param memberRepository the repository used to check member IDs.
     * @param librarianService the service used to check librarian IDs.
     * @throws NullPointerException if either dependency is null.
     */
    public UserIdGenerator(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    public UserIdGenerator() {
        this.userService = new UserService();
    }

    /**
     * Generates a user ID that is absent from both supported account stores.
     *
     * @return a new globally unique user ID for the current application instance.
     */
    public String generate() {
        String userId;
        do {
            userId = "USER-" + UUID.randomUUID();
        } while (userService.isUserIdInUse(userId));

        return userId;
    }
}
