package libconnect.storage.file;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import libconnect.models.AccountStatus;
import libconnect.models.Member;
import libconnect.models.User;
import libconnect.storage.FileManager;
import libconnect.storage.repositories.UserRepository;
import libconnect.util.ValidationUtils;

/**
 * Provides JSON file-backed persistence for all supported user accounts.
 *
 * <p>User records are stored in one file and use a role discriminator. This allows email
 * uniqueness to be checked across members and future user roles in one place.</p>
 */
public class FileUserRepository extends FileRepositorySupport implements UserRepository {
    private static final Path DEFAULT_DATA_FILE = Path.of("data", "users.json");
    private static final String USER_ID_FIELD = "userId";
    private static final String ROLE_FIELD = "role";
    private static final String NAME_FIELD = "name";
    private static final String EMAIL_FIELD = "email";
    private static final String PASSWORD_HASH_FIELD = "passwordHash";
    private static final String STATUS_FIELD = "status";
    private static final String MEMBERSHIP_ID_FIELD = "membershipId";
    private static final String REGISTRATION_DATE_FIELD = "registrationDate";
    private static final String USER_ROLE = "USER";
    private static final String MEMBER_ROLE = "MEMBER";

    /**
     * Creates a repository backed by {@code data/users.json}.
     *
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileUserRepository() {
        this(new FileManager(), DEFAULT_DATA_FILE);
    }

    /**
     * Creates a repository backed by the supplied file.
     *
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if {@code dataFile} is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileUserRepository(Path dataFile) {
        this(new FileManager(), dataFile);
    }

    /**
     * Creates a repository with explicit storage dependencies.
     *
     * @param fileManager the manager used for file operations.
     * @param dataFile the JSON file used for persistence.
     * @throws NullPointerException if either argument is null.
     * @throws IllegalStateException if the data file cannot be created.
     */
    public FileUserRepository(FileManager fileManager, Path dataFile) {
        super(fileManager, dataFile, "users");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code userId} is blank.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public Optional<User> findByUserId(String userId) {
        String requiredUserId = ValidationUtils.requireNonBlank(userId, USER_ID_FIELD);

        return findFirst(this::parseUsers, user -> user.getUserId().equals(requiredUserId));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code email} is blank.
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public Optional<User> findByEmail(String email) {
        String requiredEmail = normalizeEmail(email);

        return findFirst(this::parseUsers,
                user -> normalizeEmail(user.getEmail()).equals(requiredEmail));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the data file cannot be read or is invalid.
     */
    @Override
    public List<User> findAll() {
        return readAllRecords(this::parseUsers);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if {@code user} is null.
     * @throws IllegalArgumentException if the user type is unsupported or conflicts with another
     *         user's ID or email.
     * @throws IllegalStateException if the data file cannot be read or written.
     */
    @Override
    public void save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        requireSupportedUserType(user);

        List<User> users = readRecords(this::parseUsers);
        upsert(users, user, existingUser -> existingUser.getUserId().equals(user.getUserId()));

        validateUsers(users);
        writeRecords(users, this::toJson);
    }

    /**
     * Parses a JSON array containing user records.
     *
     * @param json the JSON content to parse.
     * @return the parsed users.
     * @throws IllegalArgumentException if the JSON structure or a record is invalid.
     */
    private List<User> parseUsers(String json) {
        JsonParser parser = new JsonParser(json);
        List<User> users = parser.parseUserArray();
        validateUsers(users);
        return users;
    }

    /**
     * Validates identifiers and normalized email addresses across all users.
     *
     * @param users the users to validate.
     * @throws IllegalArgumentException if an identifier or email is duplicated.
     */
    private void validateUsers(List<User> users) {
        Set<String> userIds = new HashSet<>();
        Set<String> emails = new HashSet<>();
        Set<String> membershipIds = new HashSet<>();

        for (User user : users) {
            if (!userIds.add(user.getUserId())) {
                throw new IllegalArgumentException("Duplicate userId in user data: "
                        + user.getUserId());
            }

            String normalizedEmail = normalizeEmail(user.getEmail());
            if (!emails.add(normalizedEmail)) {
                throw new IllegalArgumentException("Duplicate email in user data: "
                        + user.getEmail());
            }

            if (user instanceof Member member && !membershipIds.add(member.getMembershipId())) {
                throw new IllegalArgumentException("Duplicate membershipId in user data: "
                        + member.getMembershipId());
            }
        }
    }

    /**
     * Serializes users as a readable JSON array.
     *
     * @param users the users to serialize.
     * @return the JSON representation of the users.
     */
    private String toJson(List<User> users) {
        return JsonWriter.toJsonArray(users, this::toJsonObject);
    }

    /**
     * Serializes one user as a JSON object.
     *
     * @param user the user to serialize.
     * @return the JSON object representation of the user.
     */
    private String toJsonObject(User user) {
        String role = user instanceof Member ? MEMBER_ROLE : USER_ROLE;
        String commonFields = "  {\n"
                + "    \"" + USER_ID_FIELD + "\": " + JsonWriter.quote(user.getUserId()) + ",\n"
                + "    \"" + ROLE_FIELD + "\": " + JsonWriter.quote(role) + ",\n"
                + "    \"" + NAME_FIELD + "\": " + JsonWriter.quote(user.getName()) + ",\n"
                + "    \"" + EMAIL_FIELD + "\": " + JsonWriter.quote(user.getEmail()) + ",\n"
                + "    \"" + PASSWORD_HASH_FIELD + "\": " + JsonWriter.quote(user.getPasswordHash()) + ",\n"
                + "    \"" + STATUS_FIELD + "\": " + JsonWriter.quote(user.getStatus().name());

        if (user instanceof Member member) {
            return commonFields + ",\n"
                    + "    \"" + MEMBERSHIP_ID_FIELD + "\": "
                    + JsonWriter.quote(member.getMembershipId()) + ",\n"
                    + "    \"" + REGISTRATION_DATE_FIELD + "\": "
                    + JsonWriter.quote(member.getRegistrationDate().toString()) + "\n"
                    + "  }";
        }

        return commonFields + "\n"
                + "  }";
    }

    /**
     * Validates that the repository can serialize the supplied user type.
     *
     * @param user the user to validate.
     * @throws IllegalArgumentException if the user type is not supported.
     */
    private void requireSupportedUserType(User user) {
        if (user.getClass() != User.class && !(user instanceof Member)) {
            throw new IllegalArgumentException("Unsupported user type: "
                    + user.getClass().getName());
        }
    }

    /**
     * Normalizes an email for identity comparisons.
     *
     * @param email the email to normalize.
     * @return the normalized email.
     * @throws IllegalArgumentException if the email is blank.
     */
    private String normalizeEmail(String email) {
        return ValidationUtils.requireNonBlank(email, EMAIL_FIELD).toLowerCase(Locale.ROOT);
    }

    /**
     * Parses the limited JSON structure used by this repository.
     */
    private static final class JsonParser extends JsonReader {

        /**
         * Creates a parser for JSON content.
         *
         * @param json the JSON content to parse.
         */
        private JsonParser(String json) {
            super(json, "user");
        }

        /**
         * Parses the root JSON array into users.
         *
         * @return the parsed users.
         * @throws IllegalArgumentException if the JSON is invalid.
         */
        private List<User> parseUserArray() {
            List<User> users = new ArrayList<>();
            skipWhitespace();
            expect('[');
            skipWhitespace();

            if (consume(']')) {
                ensureEnd();
                return users;
            }

            while (true) {
                users.add(parseUser());
                skipWhitespace();

                if (consume(']')) {
                    ensureEnd();
                    return users;
                }

                expect(',');
            }
        }

        /**
         * Parses one user JSON object and dispatches by role.
         *
         * @return the parsed user.
         * @throws IllegalArgumentException if the object or role is invalid.
         */
        private User parseUser() {
            skipWhitespace();
            expect('{');
            skipWhitespace();

            String userId = null;
            String role = null;
            String name = null;
            String email = null;
            String passwordHash = null;
            String status = null;
            String membershipId = null;
            String registrationDate = null;

            if (!consume('}')) {
                while (true) {
                    String fieldName = parseString();
                    skipWhitespace();
                    expect(':');

                    switch (fieldName) {
                    case USER_ID_FIELD -> userId = parseString();
                    case ROLE_FIELD -> role = parseString();
                    case NAME_FIELD -> name = parseString();
                    case EMAIL_FIELD -> email = parseString();
                    case PASSWORD_HASH_FIELD -> passwordHash = parseString();
                    case STATUS_FIELD -> status = parseString();
                    case MEMBERSHIP_ID_FIELD -> membershipId = parseString();
                    case REGISTRATION_DATE_FIELD -> registrationDate = parseString();
                    default -> skipValue();
                    }

                    skipWhitespace();
                    if (consume('}')) {
                        break;
                    }
                    expect(',');
                    skipWhitespace();
                }
            }

            if (userId == null || role == null || name == null || email == null
                    || passwordHash == null || status == null) {
                throw new IllegalArgumentException("User record is missing a required field");
            }

            try {
                AccountStatus accountStatus = AccountStatus.valueOf(status);
                return switch (role) {
                case USER_ROLE -> new User(userId, name, email, passwordHash, accountStatus);
                case MEMBER_ROLE -> parseMember(userId, name, email, passwordHash, accountStatus,
                        membershipId, registrationDate);
                default -> throw new IllegalArgumentException("Unsupported user role: " + role);
                };
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid user record", exception);
            }
        }

        /**
         * Creates a member from its role-specific persisted fields.
         *
         * @param userId the user identifier.
         * @param name the member name.
         * @param email the member email.
         * @param passwordHash the password hash.
         * @param status the account status.
         * @param membershipId the membership identifier.
         * @param registrationDate the registration date text.
         * @return the restored member.
         */
        private Member parseMember(String userId, String name, String email, String passwordHash,
                                   AccountStatus status, String membershipId,
                                   String registrationDate) {
            if (membershipId == null || registrationDate == null) {
                throw new IllegalArgumentException("Member record is missing a required field");
            }

            return new Member(userId, name, email, passwordHash, membershipId,
                    LocalDate.parse(registrationDate), status);
        }

    }
}
