package libconnect.services;

import java.util.Objects;
import java.util.UUID;

import libconnect.storage.repositories.MemberRepository;

/** Generates user IDs that are unique across member and librarian accounts. */
public final class UserIdGenerator {
    private final MemberRepository memberRepository;
    private final LibrarianService librarianService;

    /**
     * Creates a generator backed by the member repository and librarian service.
     *
     * @param memberRepository the repository used to check member IDs.
     * @param librarianService the service used to check librarian IDs.
     * @throws NullPointerException if either dependency is null.
     */
    public UserIdGenerator(MemberRepository memberRepository, LibrarianService librarianService) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
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
        } while (memberRepository.findByUserId(userId).isPresent()
                || librarianService.findByUserId(userId).isPresent());

        return userId;
    }
}
