package libconnect.services;

import java.util.Objects;
import java.util.UUID;

import libconnect.models.Member;
import libconnect.storage.file.FileMemberRepository;

/** Provides member registration, profile, password, and account-status operations. */
public class MemberService {
    private final FileMemberRepository memberRepository;
    private final LibrarianService librarianService;
    private final UserIdGenerator userIdGenerator;

    /** Creates a service backed by the default member data file. */
    public MemberService() {
        this(new FileMemberRepository());
    }

    /**
     * Creates a service backed by the supplied member repository.
     *
     * @param memberRepository the repository used to persist members.
     * @throws NullPointerException if {@code memberRepository} is null.
     */
    public MemberService(FileMemberRepository memberRepository) {
        this(memberRepository, new LibrarianService());
    }

    /**
     * Creates a service backed by the supplied member repository and librarian lookup service.
     *
     * @param memberRepository the repository used to persist members.
     * @param librarianService the service used to enforce cross-role uniqueness.
     * @throws NullPointerException if either dependency is null.
     */
    public MemberService(FileMemberRepository memberRepository, LibrarianService librarianService) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.librarianService = Objects.requireNonNull(librarianService, "librarianService");
        this.userIdGenerator = new UserIdGenerator(memberRepository, librarianService);
    }

    /**
     * Registers a new active member with a generated user ID and membership ID.
     *
     * <p>The supplied password is salted and hashed before it is persisted.</p>
     *
     * @param name the member's display name.
     * @param email the member's email address.
     * @param password the member's plaintext password.
     * @throws ServiceException if the email is already in use or password hashing fails.
     * @throws IllegalArgumentException if a supplied value is invalid.
     */
    public void registerMember(String name, String email, String password) {
        if (memberRepository.findByEmail(email).isPresent()
                || librarianService.findByEmail(email).isPresent()) {
            throw new ServiceException("Email is already in use: " + email);
        }

        String userId = userIdGenerator.generate();
        String membershipId = generateUniqueMembershipId();
        String passwordHash = PasswordHasher.hash(password);
        Member member = new Member(userId, name, email, passwordHash, membershipId);
        memberRepository.save(member);
    }

    /**
     * Deletes a member from persistence.
     *
     * @param membershipId the membership ID of the member to delete.
     * @throws NotFoundException if the member does not exist.
     */
    public void deleteMember(String membershipId) {
        if (!memberRepository.deleteById(membershipId)) {
            throw new NotFoundException("Member not found: " + membershipId);
        }
    }

    /**
     * Updates a member's name and email address.
     *
     * @param membershipId the membership ID of the member to update.
     * @param name the member's new display name.
     * @param email the member's new email address.
     * @throws NotFoundException if the member does not exist.
     * @throws ServiceException if the email belongs to another member.
     * @throws IllegalArgumentException if a supplied value is invalid.
     */
    public void updateMemberProfile(String membershipId, String name, String email) {
        Member member = findMember(membershipId);
        memberRepository.findByEmail(email).ifPresent(existingMember -> {
            if (!existingMember.getMembershipId().equals(member.getMembershipId())) {
                throw new ServiceException("Email is already in use: " + email);
            }
        });

        member.updateProfile(name, email);
        memberRepository.save(member);
    }

    /**
     * Replaces a member's stored password hash.
     *
     * @param membershipId the membership ID of the member to update.
     * @param newPassword the member's new plaintext password.
     * @throws NotFoundException if the member does not exist.
     * @throws ServiceException if password hashing fails.
     * @throws IllegalArgumentException if {@code newPassword} is blank.
     */
    public void updatePassword(String membershipId, String newPassword) {
        Member member = findMember(membershipId);
        member.updatePasswordHash(PasswordHasher.hash(newPassword));
        memberRepository.save(member);
    }

    /**
     * Deactivates a member account and persists the updated status.
     *
     * @param membershipId the membership ID of the member to deactivate.
     * @throws NotFoundException if the member does not exist.
     */
    public void deactivateMember(String membershipId) {
        Member member = findMember(membershipId);
        member.deactivateAccount();
        memberRepository.save(member);
    }

    /**
     * Activates a member account and persists the updated status.
     *
     * @param membershipId the membership ID of the member to activate.
     * @throws NotFoundException if the member does not exist.
     */
    public void activateMember(String membershipId) {
        Member member = findMember(membershipId);
        member.activateAccount();
        memberRepository.save(member);
    }

    private Member findMember(String membershipId) {
        return memberRepository.findByMembershipId(membershipId)
                .orElseThrow(() -> new NotFoundException("Member not found: " + membershipId));
    }

    private String generateUniqueMembershipId() {
        String membershipId;
        do {
            membershipId = "MEMBER-" + UUID.randomUUID();
        } while (memberRepository.findByMembershipId(membershipId).isPresent());

        return membershipId;
    }
}
