package libconnect.services;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import libconnect.models.AccountStatus;
import libconnect.models.AccountType;
import libconnect.models.Librarian;
import libconnect.models.User;
import libconnect.util.ValidationUtils;

/** Authenticates users and delegates account creation to the appropriate role service. */
public class AuthenticationService {
    private final MemberService memberService;
    private final LibrarianService librarianService;
    private final UserService userService;

    /** Creates an authentication service backed by the default member data file. */
    public AuthenticationService() {
        this(new MemberService(), new LibrarianService(), new UserService());
    }

    /**
     * Creates an authentication service with explicit role-service dependencies.
     *
     * @param memberRepository the repository used for member lookup and registration.
     * @param librarianRepository the repository used for librarian lookup and registration.
     * @param userService the service used for user lookup and registration.
     * @throws NullPointerException if either dependency is null.
     */
    public AuthenticationService(MemberService memberService, LibrarianService librarianService, UserService userService) {
        this.memberService = Objects.requireNonNull(memberService, "memberService");
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.userService = Objects.requireNonNull(userService, "userService");
    }

    /**
     * Authenticates an account of the selected type.
     *
     * @param accountType the account type selected by the user.
     * @param email the account email address.
     * @param password the plaintext account password.
     * @return the authenticated user.
     * @throws AuthenticationException if the credentials are invalid, the account is deactivated,
     *         or librarian persistence is not implemented.
     * @throws IllegalArgumentException if the email or password is blank.
     * @throws NullPointerException if {@code accountType} is null.
     */
    public User authenticate(AccountType accountType, String email, String password) {
        Objects.requireNonNull(accountType, "accountType cannot be null");
        String requiredEmail = ValidationUtils.requireNonBlank(email, "email");
        ValidationUtils.requireNonBlank(password, "password");

        User user = findUser(requiredEmail)
                .orElseThrow(() -> new AuthenticationException("Invalid email or password."));
        if (!PasswordHasher.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password.");
        }
        if (!user.isActive()) {
            throw new AuthenticationException("This account is deactivated and cannot log in.");
        }

        return user;
    }

    /**
     * Registers an account of the selected type.
     *
     * Registration is delegated to the service for the selected account type.
     *
     * @param accountType the account type selected by the user.
     * @param name the account holder's name.
     * @param email the account email address.
     * @param password the plaintext account password.
     * @return the newly registered account.
     * @throws IllegalArgumentException if a supplied value is invalid.
     * @throws NullPointerException if {@code accountType} is null.
     */
    public User registerMember(String name, String email, String password) {
        memberService.registerMember(name, email, password);
        try {
            return memberService.findMemberByEmail(email);
        } catch (NotFoundException e) {
            throw new ServiceException("Member registration completed but the account could not be loaded.");
        }
    }

    public User registerLibrarian(String employeeId, String name, String email, String password) {
        UserIdGenerator userIdGenerator = new UserIdGenerator();
        librarianService.register(new Librarian(userIdGenerator.generate(), employeeId, name, email, PasswordHasher.hash(password), AccountStatus.ACTIVE));
        try {
            return librarianService.requireActive(employeeId);
        } catch (IllegalStateException e) {
            throw new ServiceException("Librarian registration completed but the account could not be loaded.");
        }
    }

    private Optional<User> findUser(String email) {
        return userService.findUserByEmail(email);
    }
}
