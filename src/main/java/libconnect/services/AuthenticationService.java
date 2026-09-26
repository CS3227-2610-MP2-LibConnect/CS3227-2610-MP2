package libconnect.services;

import java.util.Objects;
import java.util.Optional;

import libconnect.models.AccountType;
import libconnect.models.User;
import libconnect.storage.file.FileMemberRepository;
import libconnect.util.ValidationUtils;

/** Authenticates users and delegates account creation to the appropriate role service. */
public class AuthenticationService {
    private final FileMemberRepository memberRepository;
    private final MemberService memberService;
    private final LibrarianService librarianService;

    /** Creates an authentication service backed by the default member data file. */
    public AuthenticationService() {
        this(new FileMemberRepository(), new LibrarianService());
    }

    /**
     * Creates an authentication service with explicit role-service dependencies.
     *
     * @param memberRepository the repository used for member lookup and registration.
     * @param librarianService the service used for librarian lookup.
     * @throws NullPointerException if either dependency is null.
     */
    public AuthenticationService(FileMemberRepository memberRepository,
                                 LibrarianService librarianService) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.memberService = new MemberService(memberRepository, librarianService);
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
        if (accountType == AccountType.LIBRARIAN) {
            throw new AuthenticationException("Librarian authentication is not supported yet.");
        }

        User user = findUser(accountType, requiredEmail)
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
     * Member registration is delegated to {@link MemberService}. Librarian registration is
     * rejected until a librarian model and persistence service are available.
     *
     * @param accountType the account type selected by the user.
     * @param name the account holder's name.
     * @param email the account email address.
     * @param password the plaintext account password.
     * @return the newly registered member.
     * @throws ServiceException if librarian registration is not yet supported.
     * @throws IllegalArgumentException if a supplied value is invalid.
     * @throws NullPointerException if {@code accountType} is null.
     */
    public User register(AccountType accountType, String name, String email, String password) {
        Objects.requireNonNull(accountType, "accountType cannot be null");
        if (accountType == AccountType.LIBRARIAN) {
            // TODO: Implement librarian registration once a librarian model and persistence service are available.
            throw new ServiceException("Librarian account registration is not supported yet.");
        }

        memberService.registerMember(name, email, password);
        return memberRepository.findByEmail(email)
                .map(member -> (User) member)
                .orElseThrow(() -> new ServiceException(
                        "Member registration completed but the account could not be loaded."));
    }

    private Optional<User> findUser(AccountType accountType, String email) {
        return switch (accountType) {
        case MEMBER -> memberRepository.findByEmail(email).map(member -> (User) member);
        case LIBRARIAN -> librarianService.findByEmail(email);
        };
    }
}
