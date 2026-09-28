package libconnect.storage.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.storage.StorageManager;
import libconnect.storage.repositories.MemberRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for library members.
 */
public class FileMemberRepository extends AbstractFileRepository<Member> implements MemberRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "members.json");

    /**
     * Creates a member repository backed by {@code data/members.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileMemberRepository() {
        this(new StorageManager(DEFAULT_DATA_FILE.getParent()), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a member repository backed by the supplied member data file.
     *
     * @param dataFile the JSON file used for member persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileMemberRepository(Path dataFile) {
        this(new StorageManager(dataFile.getParent()), dataFile);
    }

    /**
     * Creates a member repository with explicit storage dependencies.
     *
     * @param storageManager the manager used for storage operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     */
    public FileMemberRepository(StorageManager storageManager, Path dataFile) {
        super(storageManager, dataFile, Member.class);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code membershipId} is blank.
     * @throws IllegalStateException if the member data file cannot be read.
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
     * @throws IllegalStateException if the member data file cannot be read.
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
     * @throws IllegalStateException if the member data file cannot be read.
     */
    @Override
    public Optional<Member> findByEmail(String email) {
        String requiredEmail = ValidationUtils.requireNonBlank(email, "email").toLowerCase(Locale.ROOT);

        return findAll().stream()
                .filter(member -> member.getEmail().toLowerCase(Locale.ROOT).equals(requiredEmail))
                .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code name} is blank.
     * @throws IllegalStateException if the member data file cannot be read.
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
     * @throws IllegalStateException if the member data file cannot be read.
     */
    @Override
    public List<Member> findByStatus(AccountStatus status) {
        Objects.requireNonNull(status, "status cannot be null");

        return findAll().stream()
                .filter(member -> member.getStatus() == status)
                .toList();
    }
}
