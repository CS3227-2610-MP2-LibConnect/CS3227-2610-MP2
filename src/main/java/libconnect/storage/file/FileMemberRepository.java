package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.storage.repositories.MemberRepository;
import libconnect.storage.repositories.UserRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides member-specific access to the shared user repository.
 */
public class FileMemberRepository implements MemberRepository {
    private final UserRepository userRepository;

    /**
     * Creates a member repository backed by {@code data/users.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileMemberRepository() {
        this(new FileUserRepository());
    }

    /**
     * Creates a member repository backed by the supplied user data file.
     *
     * @param dataFile the JSON file used for shared user persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileMemberRepository(Path dataFile) {
        this(new FileUserRepository(dataFile));
    }

    /**
     * Creates a member repository with an explicit user repository dependency.
     *
     * @param userRepository the shared user repository.
     * @throws NullPointerException if {@code userRepository} is null.
     */
    public FileMemberRepository(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository,
                "userRepository cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code membershipId} is blank.
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public Optional<Member> findByMembershipId(String membershipId) {
        String requiredMembershipId = ValidationUtils.requireNonBlank(membershipId, "membershipId");

        return findAll().stream()
                .filter(member -> member.getMembershipId().equals(requiredMembershipId))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code userId} is blank.
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public Optional<Member> findByUserId(String userId) {
        String requiredUserId = ValidationUtils.requireNonBlank(userId, "userId");

        return findAll().stream()
                .filter(member -> member.getUserId().equals(requiredUserId))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code email} is blank.
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public Optional<Member> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(Member.class::isInstance)
                .map(Member.class::cast);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public List<Member> findAll() {
        return userRepository.findAll().stream()
                .filter(Member.class::isInstance)
                .map(Member.class::cast)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code name} is blank.
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public List<Member> findByName(String name) {
        String requiredName = ValidationUtils.requireNonBlank(name, "name").toLowerCase(Locale.ROOT);

        return findAll().stream()
                .filter(member -> member.getName().toLowerCase(Locale.ROOT).contains(requiredName))
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code status} is null.
     * @throws IllegalStateException if the shared data file cannot be read.
     */
    @Override
    public List<Member> findByStatus(AccountStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return findAll().stream()
                .filter(member -> member.getStatus() == status)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code member} is null.
     * @throws IllegalArgumentException if the member conflicts with another user's ID or email.
     * @throws IllegalStateException if the shared data file cannot be read or written.
     */
    @Override
    public void save(Member member) {
        userRepository.save(Objects.requireNonNull(member, "member cannot be null"));
    }

}
