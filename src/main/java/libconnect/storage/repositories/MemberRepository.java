package libconnect.storage.repositories;

import java.util.List;
import java.util.Optional;

import libconnect.models.AccountStatus;
import libconnect.models.Member;

/**
 * Defines member-specific persistence operations.
 */
public interface MemberRepository {
    /**
     * Finds a member by the stable membership identifier.
     *
     * @param membershipId the membership identifier to find.
     * @return the matching member, or an empty optional if no member exists.
     */
    Optional<Member> findByMembershipId(String membershipId);

    /**
     * Finds a member by the inherited user identifier.
     *
     * @param userId the user identifier to find.
     * @return the matching member, or an empty optional if no member exists.
     */
    Optional<Member> findByUserId(String userId);

    /**
     * Finds a member by email address.
     *
     * @param email the email address to find.
     * @return the matching member, or an empty optional if no member exists.
     */
    Optional<Member> findByEmail(String email);

    /**
     * Returns every persisted member.
     *
     * @return all persisted members.
     */
    List<Member> findAll();

    /**
     * Finds members whose names contain the supplied text, ignoring case.
     *
     * @param name the name text to search for.
     * @return all members with matching names.
     */
    List<Member> findByName(String name);

    /**
     * Finds members with the supplied account status.
     *
     * @param status the account status to search for.
     * @return all members with the supplied status.
     */
    List<Member> findByStatus(AccountStatus status);

    /**
     * Inserts a member or replaces the existing member with the same user ID.
     *
     * @param member the member to persist.
     */
    void save(Member member);
}
